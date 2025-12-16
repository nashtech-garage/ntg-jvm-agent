'use client';

import { useState, useRef } from 'react';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Upload, X } from 'lucide-react';
import { useToaster } from '@/contexts/ToasterContext';

interface AvatarUploadProps {
  currentAvatar?: string;
  agentName: string;
  onAvatarChange: (avatarBase64: string | undefined) => void;
}

export default function AvatarUpload({
  currentAvatar,
  agentName,
  onAvatarChange,
}: AvatarUploadProps) {
  const { showError } = useToaster();
  const [preview, setPreview] = useState<string | undefined>(currentAvatar);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    // Validate file type
    if (!file.type.startsWith('image/')) {
      showError('Please select an image file');
      return;
    }

    // Validate file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      showError('Image size must be less than 5MB');
      return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
      const base64String = e.target?.result as string;
      setPreview(base64String);
      onAvatarChange(base64String);
    };
    reader.readAsDataURL(file);
  };

  const handleRemove = () => {
    setPreview(undefined);
    onAvatarChange(undefined);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <div className="flex flex-col items-center gap-4">
      <div className="relative">
        <Avatar className="h-24 w-24 border-2 border-dashed border-gray-300 rounded-full">
          <AvatarImage src={preview} alt={agentName} />
          <AvatarFallback className="text-xl font-bold">
            {agentName?.charAt(0)?.toUpperCase() || 'A'}
          </AvatarFallback>
        </Avatar>
        {preview && (
          <button
            onClick={handleRemove}
            className="absolute top-0 right-0 bg-red-500 text-white rounded-full p-1 hover:bg-red-600"
            type="button"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>

      <div className="flex gap-2">
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          onChange={handleFileSelect}
          className="hidden"
        />
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => fileInputRef.current?.click()}
        >
          <Upload className="h-4 w-4 mr-2" />
          Upload Avatar
        </Button>
        {preview && (
          <Button type="button" variant="outline" size="sm" onClick={handleRemove}>
            <X className="h-4 w-4 mr-2" />
            Clear
          </Button>
        )}
      </div>
      <p className="text-xs text-muted-foreground">PNG, JPG up to 5MB</p>
    </div>
  );
}
