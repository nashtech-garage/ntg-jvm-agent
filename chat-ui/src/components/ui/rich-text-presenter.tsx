import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/cjs/styles/prism';
import { Button } from './button';

function CodeComponent({
  inline,
  className,
  children,
  ...props
}: {
  inline?: boolean;
  className?: string;
  children?: React.ReactNode;
}) {
  const match = /language-(\w+)/.exec(className || '');
  return !inline && match ? (
    <SyntaxHighlighter
      language={match[1]}
      style={oneDark}
      PreTag="div"
      className="rounded-lg"
      {...props}
    >
      {String(children).replace(/\n$/, '')}
    </SyntaxHighlighter>
  ) : (
    <code className="bg-surface-soft px-1 py-0.5 rounded text-foreground" {...props}>
      {children}
    </code>
  );
}

export default function RichTextPresenter({ content }: Readonly<{ content: string }>) {
  return (
    <div className="prose max-w-none rounded-lg bg-surface-muted p-4 text-foreground prose-headings:text-foreground prose-strong:text-foreground prose-a:text-primary prose-code:bg-transparent">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          code: CodeComponent,
          a: ({ href, children }: { href?: string; children?: React.ReactNode }) => (
            <Button
              variant="link"
              type="button"
              onClick={() => href && window.open(href, '_blank', 'noopener,noreferrer')}
              title={href}
              aria-label={href ? `Open link: ${href}` : 'Open link'}
              className="text-sm text-muted-foreground px-1 py-0.5 leading-[1.2] rounded hover:text-foreground bg-surface-muted hover:bg-surface-soft cursor-pointer"
            >
              {children}
            </Button>
          ),
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}
