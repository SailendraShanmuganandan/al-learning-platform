export interface StudentErrorAction {
  label: string;
  href?: string;
  onClick?: () => void;
}

export interface StudentErrorDetails {
  title: string;
  message: string;
  code?: string;
  status?: number;
  unansweredQuestionIds?: number[];
  action?: StudentErrorAction;
}

export interface BackendApiError {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  code?: string;
  unansweredQuestionIds?: number[];
}

export function mapStudentError(err: unknown): StudentErrorDetails {
  if (!err) {
    return {
      title: 'Unexpected Error',
      message: 'An unexpected issue occurred. Please try again.',
    };
  }

  const apiErr = err as BackendApiError;
  const code = (apiErr.code || apiErr.error || '').toUpperCase();
  const status = apiErr.status;

  if (code === 'EMAIL_NOT_VERIFIED' || (status === 403 && code.includes('EMAIL'))) {
    return {
      title: 'Email Verification Required',
      message: 'Please verify your email address to unlock assessments and full learning features.',
      code: 'EMAIL_NOT_VERIFIED',
      status: 403,
      action: {
        label: 'Verify Email',
        href: '/dashboard',
      },
    };
  }

  if (code === 'NOT_ENOUGH_QUESTIONS') {
    return {
      title: 'Quiz Not Ready',
      message: "This quiz isn't ready yet — there aren't enough questions available in the question bank.",
      code: 'NOT_ENOUGH_QUESTIONS',
      status: 409,
    };
  }

  if (code === 'ATTEMPT_ALREADY_SUBMITTED' || code === 'ASSESSMENT_ALREADY_SUBMITTED') {
    return {
      title: 'Already Submitted',
      message: 'This attempt has already been submitted and graded.',
      code: 'ALREADY_SUBMITTED',
      status: 409,
    };
  }

  if (code === 'STUDY_SESSION_ENDED') {
    return {
      title: 'Study Session Ended',
      message: 'This study session has already ended or time expired.',
      code: 'STUDY_SESSION_ENDED',
      status: 409,
    };
  }

  if (code === 'UNANSWERED_QUESTIONS') {
    return {
      title: 'Unanswered Questions',
      message: 'Please answer all questions before submitting your attempt.',
      code: 'UNANSWERED_QUESTIONS',
      status: 400,
      unansweredQuestionIds: apiErr.unansweredQuestionIds,
    };
  }

  if (code === 'INVALID_ANSWER') {
    return {
      title: 'Invalid Answer',
      message: 'One or more submitted answers are invalid or do not match this quiz.',
      code: 'INVALID_ANSWER',
      status: 400,
    };
  }

  if (code === 'NO_MISTAKES_TO_PRACTICE') {
    return {
      title: 'No Mistakes to Practise',
      message: 'You currently have no active mistakes ready for practice in this scope! Keep learning.',
      code: 'NO_MISTAKES_TO_PRACTICE',
      status: 409,
    };
  }

  if (code === 'ALREADY_REPORTED') {
    return {
      title: 'Already Reported',
      message: 'You have already submitted a report for this question. Thank you for your feedback.',
      code: 'ALREADY_REPORTED',
      status: 409,
    };
  }

  if (code === 'NOT_ANSWERED_BY_USER') {
    return {
      title: 'Question Not Answered',
      message: 'You can only report questions that you have previously attempted.',
      code: 'NOT_ANSWERED_BY_USER',
      status: 403,
    };
  }

  if (status === 401) {
    return {
      title: 'Session Expired',
      message: 'Your session has expired. Please sign in again to continue.',
      code: 'UNAUTHORIZED',
      status: 401,
      action: {
        label: 'Sign In',
        href: '/login',
      },
    };
  }

  if (status === 404) {
    return {
      title: 'Not Found',
      message: apiErr.message || 'The requested quiz, chapter, or session could not be found.',
      code: 'NOT_FOUND',
      status: 404,
    };
  }

  if (status === 409) {
    return {
      title: 'Conflict',
      message: apiErr.message || 'A conflicting state occurred. Please refresh the page and try again.',
      code: 'CONFLICT',
      status: 409,
    };
  }

  return {
    title: 'Error',
    message: apiErr.message || 'Operation failed. Please try again.',
    code: code || 'ERROR',
    status,
  };
}

