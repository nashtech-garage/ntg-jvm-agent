# Front End Guidelines

- These instructions are applied to projects: `admin-ui\*` and `chat-ui\*`

## File Organization

### Colocation Principle

- Prefer colocation: if logic or components only serve a single route, keep them in that route folder; promote to `components/`, `utils/`, etc., only when reused.
- Related Files Together: Keep files that change together in the same directory.
- Use path alias `@/*` points to src directory (see `tsconfig.json`); prefer these aliases rather than relative paths.
- Consistent Structure: Maintain consistent folder structure across similar features
- Shallow Nesting: Avoid deeply nested folder structures
- Clear Separation: Separate concerns clearly (UI, logic, data, types)
- Easy Navigation: Structure should make it easy to find and modify related code

#### Directory Structure

- Pages live in `page.tsx`; shared layouts in `layout.tsx`; colocate `loading.tsx`/`error.tsx` as needed.
- API endpoints belong in `app/api/<name>/route.ts` and should delegate logic to `services/`, `libs/`, or `actions/` rather than embedding heavy logic in the handler.

```
project-root/
|-- src/
  |-- app/                 # Next.js app routes
      |-- api/             # API routes (route.ts per endpoint)
      |-- <your-page>/     # App pages (admin, login, logout, etc.)
        |-- layout.tsx     # Page layout
        |-- page.tsx       # Your page
      |-- layout.tsx       # Root layout
      |-- page.tsx         # Home page
  |-- components/          # Reusable UI components
      |-- ui/              # Basic UI components (buttons, inputs, etc.)
      |-- icon/            # Wrap SVG inside of components
      |-- external-script/ # Define external embedded scripts (<Script />)
      |-- <other-file>/    # Specific UI components in page (auth, admin, chat, etc.)
  |-- constants/           # Static configuration
      |-- siteConfig.ts    # Centralize site configuration / environment
      |-- url.ts           # Include all path or URL in both internal and external links
      |-- <other-file>.ts  # All left constants (fixed value, hardcode string, enum, etc.)
  |-- contexts/            # React context providers
  |-- actions/             # Server action helpers
  |-- hooks/               # Reusable client hooks
  |-- libs/                # SDK setup
  |-- services/            # API calls (fetch)
  |-- models/              # Data model definitions
  |-- types/               # TypeScript definitions
  |-- utils/               # Pure utilities (logging, css, fetcher, etc.)
  |-- middleware.ts        # Handle authorization or other global request handling
  |-- styles/              # Global styles (globals.css, Tailwind layers)
  |-- __mock__/            # Mock data for UI development or UT
```

### Naming Conventions

- Components: PascalCase (e.g., `UserProfile.tsx` or `AuthContext.tsx`).
- Non-component files: camelCase (e.g., `chatHistory.service.ts`, `menuConfig.ts`).
- Directories: kebab-case.
- Route segments: kebab-case folders; use `(group)` folders only when intentionally grouping routes.
- Context/providers end with `Provider`; hooks start with `use`; stores end with `Store`.

## Implementation Guidelines

- Follow React/Next.JS for application implementation
- Make sure that eslint rules are passed when commit codes

### Component Design

- **Functional Components & Hooks:** Prefer **functional components with React Hooks**. Avoid class components unless explicitly for error boundaries.
- **Single Responsibility:** Each component should ideally have one primary responsibility. **Components should be kept small and focused.**
- **Component Naming:** Use `PascalCase` for all component names (e.g., `MyButton`, `UserAvatar`).
- **Props:**
  - Use `camelCase` for prop names.
  - Destructure props in the component's function signature.
  - Provide clear `interface` or `type` definitions for props in TypeScript.
- **Immutability:** Never mutate props or state directly. Always create new objects or arrays for updates.
- **Fragments:** Use `<>` or `React.Fragment` to avoid unnecessary DOM wrapper elements.
- **Custom Hooks:** Extract reusable stateful logic into **custom hooks** (e.g., `useDebounce`, `useLocalStorage`).
- **UI Components:** Use [@radix-ui/*](https://www.radix-ui.com/themes/docs/overview/getting-started) for building UI components to ensure consistency and accessibility.

### State Management

- **Local State:** Use `useState` for component-level state.
- **Global State:** For global or shared state, prefer **React Context API** or a dedicated state management library (Zustand). Avoid prop drilling.

### API routes

- **Resource Naming:** use plural nouns for collections (`/api/users`, `/api/chat`); avoid use verbs in URL paths and use kebab-case for multi-word resources (`/api/user-profile`)
- **HTTP Methods:** GET for retrieval, POST for creation, PUT for full updates, PATCH for partial updates, DELETE for removal
- **Nesting:** Consider flattening deeply nested resources. If nesting, limit to 2-3 levels maximum

### Error handling and logging

- **Log errors appropriately:** Avoid calling console[log|error|info] directly. Always log errors for debugging through `utils\logger.ts`.

### Styling

- **Consistent Approach:** use Tailwind CSS v4 or later.
- **Scoped Styles:** Ensure styles are scoped to avoid global conflicts.

### Performance

- **Keys:** Always provide a unique and stable `key` prop when mapping over lists. Do not use array `index` as a key if the list can change.
- **Image Optimization:** Always use `next/image` component for images.
- **Font Optimization:** Use `next/font` for optimizing fonts.
- **Dynamic Imports:** Use `next/dynamic` for lazy loading components to reduce initial bundle size.
