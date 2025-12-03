export const Label = ({ className, ...props }: React.LabelHTMLAttributes<HTMLLabelElement>) => {
  return (
    <label className={`block text-sm font-medium text-gray-700 ${className || ''}`} {...props} />
  );
};
