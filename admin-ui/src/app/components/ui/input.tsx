import { cn } from "@/lib/utils";

export const Input = ({ className, ...props }: React.InputHTMLAttributes<HTMLInputElement>) => {
  return (
    <input
      className={cn(
        "w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none",
        className
      )}
      {...props}
    />
  );
};
