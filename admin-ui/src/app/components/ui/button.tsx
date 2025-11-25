import { cn } from "@/lib/utils";

export const Button = ({ className, variant, ...props }: any) => {
  const base = "px-4 py-2 rounded-lg font-medium transition-colors";
  const variantClass = variant === "outline"
    ? "bg-white border border-gray-300 text-gray-700 hover:bg-gray-100"
    : "bg-blue-600 text-white hover:bg-blue-700";

  return <button className={cn(base, variantClass, className)} {...props} />;
};
