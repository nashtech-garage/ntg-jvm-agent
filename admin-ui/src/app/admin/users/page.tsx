'use client';

import EditUserModal from '@/components/modal/edit-user-modal';
import { useCallback, useEffect, useState } from 'react';
import CreateUserModal from '@/components/modal/create-user-modal';
import { User, UserPageDto } from '@/models/user';
import logger from '@/utils/logger';
import CommonConfirmModal from '@/components/modal/common-confirm-modal';
import { API_URLS } from '@/constants/constant';
import { useToaster } from '@/contexts/ToasterContext';

type DeleteUserResponse = {
  message?: string;
  error?: string;
  raw?: string;
};

export default function UserManagement() {
  const { errorToaster, successToaster } = useToaster();

  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [userToDelete, setUserToDelete] = useState<string | null>(null);

  const fetchUsers = useCallback(async () => {
    try {
      setLoading(true);
      const response = await fetch(`/api/users?page=${page}&size=10`);

      if (!response.ok) {
        throw new Error('Failed to fetch users');
      }

      const data: UserPageDto = await response.json();
      setUsers(data.users || []);
      setTotalPages(data.totalPages);
    } catch (error) {
      logger.error('Failed to fetch users:', error);
    } finally {
      setLoading(false);
    }
  }, [page]); // page is included because it's used inside fetchUsers

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const nextPage = () => {
    if (page < totalPages - 1) setPage((prev) => prev + 1);
  };

  const prevPage = () => {
    if (page > 0) setPage((prev) => prev - 1);
  };

  const updateUserStatus = async (username: string, enabled: boolean) => {
    try {
      const response = await fetch('/api/users', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, enabled }),
      });

      if (!response.ok) {
        const text = await response.text();
        logger.error(`Backend response: ${response.status}`, text);
        throw new Error('Failed to update user status');
      }
      setUsers((prev) =>
        prev.map((user) => (user.username === username ? { ...user, enabled } : user))
      );
      const action = enabled ? 'activated' : 'deactivated';
      const message = `User "${username}" has been ${action} successfully.`;
      successToaster(message);
    } catch (error) {
      logger.error('Error updating user status:', error);
      const action = enabled ? 'activate' : 'deactivate';
      const message = `Failed to ${action} user "${username}".`;
      errorToaster(message);
    }
  };

  const handleDeactivate = async (username: string) => {
    await updateUserStatus(username, false);
  };

  const handleActivate = async (username: string) => {
    await updateUserStatus(username, true);
  };
  const openDeleteModal = (username: string) => {
    setUserToDelete(username);
    setShowDeleteModal(true);
  };

  const closeDeleteModal = () => {
    setShowDeleteModal(false);
    setUserToDelete(null);
  };

  const handleConfirmDelete = async () => {
    if (!userToDelete) return;

    try {
      const query = new URLSearchParams({ username: userToDelete }).toString();
      const response = await fetch(`${API_URLS.USERS}?${query}`, {
        method: 'DELETE',
      });

      const text = await response.text();
      let jsonResult: DeleteUserResponse = {};
      try {
        jsonResult = text ? JSON.parse(text) : {};
      } catch {
        jsonResult = text ? { raw: text } : {};
      }

      if (!response.ok) {
        logger.error(
          `Failed to delete user "${userToDelete}". Status: ${response.status}`,
          jsonResult
        );
        errorToaster(jsonResult.error || `Failed to delete user "${userToDelete}".`);
        return;
      }

      setUsers((prev) => prev.filter((u) => u.username !== userToDelete));

      const message = jsonResult.message || `User "${userToDelete}" has been deleted successfully.`;
      successToaster(message);
    } catch (error) {
      logger.error('Error deleting user:', error);
      errorToaster(`Failed to delete user "${userToDelete}".`);
    } finally {
      closeDeleteModal();
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">User Management</h1>
          <p className="text-gray-600 mt-1">Manage user accounts and permissions</p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition-colors flex items-center gap-2"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M12 6v6m0 0v6m0-6h6m-6 0H6"
            />
          </svg>
          Add New User
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="flex flex-col md:flex-row gap-4">
          <div className="flex-1">
            <label htmlFor="search" className="block text-sm font-medium text-gray-700 mb-2">
              Search Users
            </label>
            <div className="relative">
              <input
                type="text"
                id="search"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Search by name or email..."
                className="w-full pl-10 pr-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <svg
                className="absolute left-3 top-2.5 h-5 w-5 text-gray-400"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                />
              </svg>
            </div>
          </div>
          <div>
            <label htmlFor="role-filter" className="block text-sm font-medium text-gray-700 mb-2">
              Filter by Role
            </label>
            <select
              id="role-filter"
              // value={filterRole}
              // onChange={(e) => setFilterRole(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="all">All Roles</option>
              <option value="admin">Admin</option>
              <option value="user">User</option>
              <option value="moderator">Moderator</option>
            </select>
          </div>
        </div>
      </div>

      {/* Users Table */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  UserName
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Name
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Email
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Roles
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {users.map((user) => (
                <tr key={user.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">{user.username}</td>
                  <td className="px-6 py-4 whitespace-nowrap">{user.name}</td>
                  <td className="px-6 py-4 whitespace-nowrap">{user.email}</td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex gap-1 flex-wrap">
                      {user.roles.map((role) => (
                        <span
                          key={role}
                          className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                            role === 'ROLE_ADMIN'
                              ? 'bg-red-100 text-red-800'
                              : 'bg-blue-100 text-blue-800'
                          }`}
                        >
                          {role.replace('ROLE_', '')}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span
                      className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                        user.enabled ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                      }`}
                    >
                      {user.enabled ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap space-x-2">
                    <button
                      onClick={() =>
                        user.enabled
                          ? handleDeactivate(user.username)
                          : handleActivate(user.username)
                      }
                      className={`px-3 py-1 rounded text-xs font-medium transition-colors ${
                        user.enabled
                          ? 'bg-red-100 text-red-700 hover:bg-red-200'
                          : 'bg-green-100 text-green-700 hover:bg-green-200'
                      }`}
                    >
                      {user.enabled ? 'Deactivate' : 'Activate'}
                    </button>
                    <button
                      className="px-3 py-1 bg-blue-100 text-blue-700 hover:bg-blue-200 rounded text-xs font-medium"
                      onClick={() => {
                        setSelectedUser(user);
                        setIsModalOpen(true);
                      }}
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => openDeleteModal(user.username)}
                      className="px-3 py-1 bg-red-100 text-red-700 hover:bg-red-200 rounded text-xs font-medium"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pagination */}
      <div className="flex justify-between items-center mt-4">
        <button
          onClick={prevPage}
          disabled={page === 0}
          className="px-4 py-2 bg-gray-100 text-gray-700 rounded disabled:opacity-50 hover:bg-gray-200"
        >
          Previous
        </button>
        <span className="text-sm text-gray-600">
          Page {page + 1} of {totalPages}
        </span>
        <button
          onClick={nextPage}
          disabled={page >= totalPages - 1}
          className="px-4 py-2 bg-gray-100 text-gray-700 rounded disabled:opacity-50 hover:bg-gray-200"
        >
          Next
        </button>
      </div>

      <CreateUserModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onUserCreated={() => {
          setPage(0);
          fetchUsers();
        }}
      />
      <CommonConfirmModal
        isOpen={showDeleteModal}
        title="Delete User"
        message={
          userToDelete
            ? `Are you sure you want to delete user "${userToDelete}"? This action cannot be undone.`
            : 'Are you sure you want to delete this user?'
        }
        confirmLabel="Delete"
        cancelLabel="Cancel"
        variant="danger"
        onConfirm={handleConfirmDelete}
        onCancel={closeDeleteModal}
      />

      {/* Edit User Modal */}
      {selectedUser && (
        <EditUserModal
          open={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          user={selectedUser}
          onSubmit={(updatedUser: User) => {
            setUsers((prev) => prev.map((u) => (u.id === updatedUser.id ? updatedUser : u)));
            setIsModalOpen(false);
          }}
        />
      )}
    </div>
  );
}
