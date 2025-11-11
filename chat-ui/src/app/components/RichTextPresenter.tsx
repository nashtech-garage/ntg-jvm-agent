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
            <a
              href={href}
              target="_blank"
              rel="noopener noreferrer"
              className="text-yellow-600 hover:underline cursor-pointer"
            >
              {children}
            </a>
          ),
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}
