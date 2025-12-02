import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/cjs/styles/prism';
import { Citation } from '@/models/chat-message';
import logger from '@/utils/logger';

interface RichTextPresenterProps {
  content: string;
  citations?: Citation[];
}

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

export default function RichTextPresenter({
  content,
  citations = [],
}: Readonly<RichTextPresenterProps>) {
  const handleDownload = async (filePath: string, fileName: string) => {
    try {
      const encodedPath = encodeURIComponent(filePath);
      const response = await fetch(`/api/file-download?path=${encodedPath}`);

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Download failed');
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      logger.error('Error downloading file:', error);
      alert('Can not download file. Please try again.');
    }
  };

  // exchange [chunkId=...] by button
  const processContent = (text: string) => {
    const citationMap = new Map(citations.map((c) => [c.chunkId, c]));

    const chunkIdRegex = /\[chunkId=([^\]]+)\]/g;

    const parts: (string | React.ReactNode)[] = [];
    let lastIndex = 0;
    let match;

    while ((match = chunkIdRegex.exec(text)) !== null) {
      // add text before match
      if (match.index > lastIndex) {
        parts.push(text.substring(lastIndex, match.index));
      }

      const chunkId = match[1];
      const citation = citationMap.get(chunkId);

      if (citation) {
        // create button for citation
        parts.push(
          <button
            key={`citation-${chunkId}-${match.index}`}
            type="button"
            onClick={() => handleDownload(citation.filePath, citation.fileName)}
            className="inline-flex items-center gap-1 text-xs bg-blue-100 text-blue-700 px-2 py-1 rounded hover:bg-blue-200 transition-colors mx-1"
            title={`Download ${citation.fileName}`}
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-3 w-3"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
              />
            </svg>
            {citation.fileName}
          </button>
        );
      } else {
        // if citation not found, keep text
        parts.push(match[0]);
      }

      lastIndex = match.index + match[0].length;
    }

    // add remain text
    if (lastIndex < text.length) {
      parts.push(text.substring(lastIndex));
    }

    return parts;
  };

  const processedContent = processContent(content);

  return (
    <div className="prose prose-blue max-w-none bg-gray-30 p-4 rounded-lg">
      <div>
        {processedContent.map((part, index) => {
          if (typeof part === 'string') {
            return (
              <ReactMarkdown
                key={index}
                remarkPlugins={[remarkGfm]}
                components={{
                  code: CodeComponent,
                  a: ({ href, children }: { href?: string; children?: React.ReactNode }) => (
                    <button
                      type="button"
                      onClick={() => href && window.open(href, '_blank', 'noopener,noreferrer')}
                      title={href}
                      aria-label={href ? `Open link: ${href}` : 'Open link'}
                      className="text-sm text-gray-500 px-1 py-0.5 leading-[1.2] rounded hover:text-gray-700 bg-gray-100 hover:bg-gray-200 cursor-pointer"
                      style={{ lineHeight: 1.2 }}
                    >
                      {children}
                    </button>
                  ),
                }}
              >
                {part}
              </ReactMarkdown>
            );
          }
          return <React.Fragment key={index}>{part}</React.Fragment>;
        })}
      </div>
    </div>
  );
}
