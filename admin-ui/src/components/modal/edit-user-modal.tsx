import { useEffect, useMemo, useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../ui/dialog';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Button } from '../ui/button';
import { User } from '../../models/user';
import { updateUser } from '@/services/users';
import { useToaster } from '@/contexts/ToasterContext';

interface EditUserModalProps {
  open: boolean;
  onClose: () => void;
  user: User;
  onSubmit: (updatedUser: User) => void;
}

export default function EditUserModal({ open, onClose, user, onSubmit }: EditUserModalProps) {
  const initialForm = useMemo(
    () => ({
      username: user?.username || '',
      name: user?.name || '',
      email: user?.email || '',
    }),
    [user?.id, open]
  );

  const [form, setForm] = useState(initialForm);
  const { showError } = useToaster();

  useEffect(() => {
    setForm(initialForm);
  }, [initialForm]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleUpdate = async () => {
    try {
      const updatedUser = await updateUser(user.id, {
        username: form.username,
        name: form.name,
        email: form.email,
      });
      onSubmit(updatedUser);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : String(err);
      showError('Update user failed: ' + errorMessage);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Edit User</DialogTitle>
        </DialogHeader>
        <div className="space-y-4 py-2">
          <div className="space-y-2">
            <Label htmlFor="username">Username</Label>
            <Input id="username" name="username" value={form.username} onChange={handleChange} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="name">Name</Label>
            <Input id="name" name="name" value={form.name} onChange={handleChange} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              name="email"
              type="email"
              value={form.email}
              onChange={handleChange}
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={handleUpdate}>Update</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
