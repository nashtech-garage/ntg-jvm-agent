'use client';

import { useDropzone } from 'react-dropzone';
import { FileItem } from '@/schemas/knowledge-schemas';
import { Button } from '@/components/ui/button';
import { v4 as uuid } from 'uuid';

interface DropzoneMultiProps {
  files: FileItem[];
  onChange: (value: FileItem[]) => void;
}

export default function DropzoneMulti({ files, onChange }: Readonly<DropzoneMultiProps>) {
  const onDrop = (acceptedFiles: File[]) => {
    const wrapped = acceptedFiles.map((file) => ({
      id: uuid(),
      file,
    }));

    onChange([...files, ...wrapped]); // ⬅ update via prop
  };

  const removeFile = (id: string) => {
    onChange(files.filter((f) => f.id !== id)); // ⬅ update via prop
  };

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    multiple: true,
  });

  return (
    <div className="space-y-4">
      {/* Dropzone */}
      <div
        {...getRootProps()}
        className={`border-2 border-dashed rounded-lg p-6 text-center cursor-pointer transition
          ${isDragActive ? 'bg-accent' : 'bg-muted/30'}`}
      >
        <input {...getInputProps()} />
        <p className="text-muted-foreground">Drag & drop files here, or click to browse</p>
      </div>

      {/* File List */}
      <div className="space-y-2">
        {files.map((item) => (
          <div key={item.id} className="flex justify-between items-center bg-muted p-2 rounded">
            <div>
              <p className="font-medium">{item.file.name}</p>
              <p className="text-xs text-muted-foreground">
                {(item.file.size / 1024 / 1024).toFixed(2)} MB
              </p>
            </div>

            <Button variant="ghost" type="button" onClick={() => removeFile(item.id)}>
              Remove
            </Button>
          </div>
        ))}
      </div>
    </div>
  );
}
