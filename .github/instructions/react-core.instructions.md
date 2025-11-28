---
applyTo: "**/*.ts,**/*.tsx,**/*.js,**/*.jsx"
---

# React Core Patterns

## Component Standards

### Functional Components Only

- Use functional components with hooks
- NO class components unless absolutely necessary for error boundaries
- Export as named exports, not default exports when possible

### Component Structure

```typescript
interface ComponentProps {
  required: string;
  optional?: number;
  children?: React.ReactNode;
}

export const ComponentName: React.FC<ComponentProps> = ({
  required,
  optional = defaultValue,
  children,
  ...props
}) => {
  // Hooks at the top
  const [state, setState] = useState();

  // Event handlers
  const handleClick = useCallback(() => {
    // handler logic
  }, [dependencies]);

  // Early returns for conditional rendering
  if (conditionalReturn) {
    return <div>Early return</div>;
  }

  return (
    <div {...props}>
      {children}
    </div>
  );
};
```

### Props and TypeScript

- Always define interface for props
- Use React.ReactNode for children
- Destructure props in function signature
- Use optional chaining for optional props
- Spread remaining props with ...props

### Hook Usage Rules

- Hooks must be at the top of components
- Never call hooks conditionally
- Use useCallback for event handlers passed as props
- Use useMemo for expensive calculations only
- Custom hooks must start with 'use'

## State Management Patterns

### Local State (useState)

```typescript
// Simple state
const [isOpen, setIsOpen] = useState(false);

// Complex state - use useReducer instead
const [formData, setFormData] = useState({
  name: "",
  email: "",
  // ... many fields - consider useReducer
});
```

### Global State Guidelines

- **useState + Context**: For simple shared state (theme, user)
- **Redux Toolkit**: For complex app state with async actions
- **Zustand**: For simple global state without boilerplate
- **React Query/TanStack Query**: For server state ALWAYS

### Context Pattern

```typescript
interface ContextType {
  value: string;
  setValue: (value: string) => void;
}

const MyContext = createContext<ContextType | null>(null);

export const useMyContext = () => {
  const context = useContext(MyContext);
  if (!context) {
    throw new Error("useMyContext must be used within MyProvider");
  }
  return context;
};
```

## Component Composition

### Compound Components

- Use for complex components with multiple parts
- Attach sub-components to main component
- Share state via context

### Children Patterns

```typescript
// Render prop pattern
interface RenderPropProps {
  children: (data: DataType) => React.ReactNode;
}

// Slot pattern
interface SlotProps {
  header?: React.ReactNode;
  content: React.ReactNode;
  footer?: React.ReactNode;
}
```

## Event Handling

### Event Handler Naming

- Use handle[Event] for component methods: handleClick, handleSubmit
- Use on[Event] for prop callbacks: onClick, onSubmit

### Event Handler Patterns

```typescript
// With useCallback for performance
const handleSubmit = useCallback((e: FormEvent) => {
  e.preventDefault();
  // logic
}, [dependencies]);

// Inline for simple handlers (no dependencies)
<button onClick={() => setCount(count + 1)}>
```

## Conditional Rendering

### Preferred Patterns

```typescript
// Use && for simple conditionals
{isLoading && <Spinner />}

// Use ternary for if/else
{isError ? <ErrorMessage /> : <SuccessMessage />}

// Use early returns for complex conditionals
if (isLoading) return <Spinner />;
if (isError) return <ErrorMessage />;
return <MainContent />;
```

## Form Handling

### Use React Hook Form

- Always use React Hook Form for forms
- Define validation schema with Zod or Yup
- Use Controller for custom components

```typescript
import { useForm, Controller } from "react-hook-form";

const {
  control,
  handleSubmit,
  formState: { errors },
} = useForm();
```

## Error Boundaries

### Required for Production

```typescript
class ErrorBoundary extends Component<PropsWithChildren> {
  state = { hasError: false };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return <ErrorFallback />;
    }
    return this.props.children;
  }
}
```

## Key Rules Summary

1. **Functional components only** with proper TypeScript interfaces
2. **Named exports** preferred over default exports
3. **Hooks at top** of components, never conditional
4. **useCallback** for event handlers passed as props
5. **React Hook Form** for all forms
6. **Error boundaries** around feature sections
7. **Early returns** for conditional rendering
8. **Proper prop destructuring** in function signature
9. **Context + custom hook** pattern for shared state
10. **React Query** for all server state management
