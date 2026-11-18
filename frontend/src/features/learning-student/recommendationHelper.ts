import type { RecommendationResponse, RecommendationTarget } from './types';
import { LEARNING_ROUTES } from './routes';

export interface RouteTarget {
  path: string;
  label: string;
}

/**
 * Pure function mapping a recommendation to a real application route and CTA label.
 */
export function recommendationToRoute(
  rec: RecommendationResponse | { type: string; target: RecommendationTarget }
): RouteTarget {
  const { type, target } = rec;

  switch (type) {
    case 'TAKE_DIAGNOSTIC':
      return {
        path: LEARNING_ROUTES.DIAGNOSTIC_START,
        label: 'Take Diagnostic Assessment',
      };

    case 'TAKE_OVERALL_QUIZ':
      return {
        path: LEARNING_ROUTES.OVERALL_QUIZ_START,
        label: 'Take Overall Quiz',
      };

    case 'PRACTICE_MISTAKES':
      if (target?.chapterId) {
        return {
          path: `${LEARNING_ROUTES.MISTAKES}?chapterId=${target.chapterId}`,
          label: target.chapterName ? `Practise Mistakes in ${target.chapterName}` : 'Practise Mistakes',
        };
      }
      return {
        path: LEARNING_ROUTES.MISTAKES,
        label: 'Practise Mistakes',
      };

    case 'TAKE_CHAPTER_QUIZ':
      if (target?.quizId) {
        return {
          path: LEARNING_ROUTES.QUIZ_START(target.quizId),
          label: target.chapterName ? `Start Quiz: ${target.chapterName}` : 'Start Chapter Quiz',
        };
      }
      if (target?.chapterId) {
        return {
          path: `/chapters/${target.chapterId}`,
          label: 'View Chapter Quiz',
        };
      }
      return {
        path: '/subjects',
        label: 'Browse Quizzes',
      };

    case 'EXPLORE_CHAPTER':
    case 'NEXT_CHAPTER':
      if (target?.chapterId) {
        return {
          path: `/chapters/${target.chapterId}`,
          label: target.chapterName ? `Explore Chapter: ${target.chapterName}` : 'Go to Chapter',
        };
      }
      return {
        path: '/subjects',
        label: 'Explore Syllabus',
      };

    case 'REVIEW_RESOURCE':
      if (target?.kind === 'VIDEO' && target.productId) {
        return {
          path: `/my-videos/${target.productId}`,
          label: 'Watch Video Lesson',
        };
      }
      if (target?.slug) {
        return {
          path: `/products/${target.slug}`,
          label: 'Review Study Material',
        };
      }
      if (target?.productId) {
        return {
          path: `/products/${target.productId}`,
          label: 'Review Resource',
        };
      }
      if (target?.chapterId) {
        return {
          path: `/chapters/${target.chapterId}`,
          label: 'View Chapter Materials',
        };
      }
      return {
        path: '/products',
        label: 'Browse Study Notes & Videos',
      };

    default:
      if (target?.kind === 'CHAPTER' && target.chapterId) {
        return { path: `/chapters/${target.chapterId}`, label: 'View Chapter' };
      }
      if (target?.kind === 'SUBJECT' && target.subjectId) {
        return { path: `/subjects/${target.subjectId}`, label: 'View Subject' };
      }
      if (target?.kind === 'SUBJECT_LIST') {
        return { path: '/subjects', label: 'View All Subjects' };
      }
      return {
        path: LEARNING_ROUTES.OVERVIEW,
        label: 'Go to Learning Center',
      };
  }
}

