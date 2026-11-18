import type React from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight } from 'lucide-react';

export interface BreadcrumbItem {
  label: string;
  path?: string;
}

export interface PageHeaderProps {
  title: React.ReactNode;
  description?: React.ReactNode;
  action?: React.ReactNode;
  breadcrumb?: React.ReactNode | BreadcrumbItem[];
  className?: string;
  children?: React.ReactNode;
}

export function PageHeader({
  title,
  description,
  action,
  breadcrumb,
  className = '',
  children,
}: PageHeaderProps) {
  const renderBreadcrumb = () => {
    if (!breadcrumb) return null;
    if (Array.isArray(breadcrumb)) {
      return (
        <nav aria-label="Breadcrumb" className="flex items-center gap-1.5 text-xs text-muted-foreground flex-wrap">
          {breadcrumb.map((item, idx) => {
            const isLast = idx === breadcrumb.length - 1;
            return (
              <span key={idx} className="flex items-center gap-1.5">
                {idx > 0 && <ChevronRight size={12} className="opacity-40" />}
                {item.path && !isLast ? (
                  <Link to={item.path} className="hover:text-foreground transition">
                    {item.label}
                  </Link>
                ) : (
                  <span className={isLast ? 'text-foreground font-medium' : ''}>
                    {item.label}
                  </span>
                )}
              </span>
            );
          })}
        </nav>
      );
    }
    return <div className="text-xs text-muted-foreground">{breadcrumb}</div>;
  };

  return (
    <div className={`mb-6 space-y-2 ${className}`}>
      {renderBreadcrumb()}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2">
            {title}
          </h1>
          {description && (
            <p className="text-sm text-muted-foreground mt-1 leading-relaxed">
              {description}
            </p>
          )}
        </div>
        {action && <div className="flex items-center gap-2 shrink-0">{action}</div>}
      </div>
      {children}
    </div>
  );
}
