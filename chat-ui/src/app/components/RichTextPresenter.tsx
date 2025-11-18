import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/cjs/styles/prism';

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
    <code className="bg-gray-200 px-1 py-0.5 rounded" {...props}>
      {children}
    </code>
  );
}

export default function RichTextPresenter({ content }: Readonly<{ content: string }>) {
  return (
    <div className="prose prose-blue max-w-none bg-gray-30 p-4 rounded-lg">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          code: CodeComponent,
          a: ({ href, children }: { href?: string; children?: React.ReactNode }) => (
            <button
              type="button"
              onClick={() => href && window.open(href, '_blank', 'noopener,noreferrer')}
              title={href}
              aria-label={href ? `Open link: ${href}` : 'Open link'}
              className="text-sm text-gray-500 px-1 py-0.5 rounded hover:text-gray-700 bg-gray-100 hover:bg-gray-200 cursor-pointer"
              style={{ lineHeight: 1.2, fontWeight: 400 }}
            >
              {children}
            </button>
          ),
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}
