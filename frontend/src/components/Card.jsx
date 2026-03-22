const variants = {
  default: "surface-card",
  strong: "surface-card surface-strong",
  ghost: "surface-card surface-ghost",
  outline: "surface-card surface-outline",
};

export default function Card({ children, className = "", variant = "default", as: Tag = "div", ...props }) {
  return (
    <Tag className={`${variants[variant] || variants.default} p-6 md:p-8 ${className}`.trim()} {...props}>
      {children}
    </Tag>
  );
}
