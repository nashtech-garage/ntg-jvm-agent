import "./globals.css";

export const metadata = {
  title: "ChatGPT UI Clone",
  description: "Next.js app with ChatGPT-style UI",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body className="h-screen flex">
        <main className="flex-1 flex flex-col">{children}</main>
      </body>
    </html>
  );
}
