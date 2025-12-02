import { ReactNode, HTMLAttributes, SVGProps } from 'react';

/**
 * Base props that all components should extend from
 */
export interface ComponentProps extends Omit<HTMLAttributes<HTMLElement>, 'className'> {
  /**
   * Additional CSS classes to apply to the component
   */
  className?: string;

  /**
   * Child elements to render inside the component
   */
  children?: ReactNode;

  /**
   * Test identifier for automated testing
   */
  testId?: string;
}

/**
 * Props for SVG icon components
 */
export interface IconProps extends Omit<SVGProps<SVGSVGElement>, 'className'> {
  /**
   * Additional CSS classes to apply to the icon
   */
  className?: string;

  /**
   * Test identifier for automated testing
   */
  testId?: string;
}
