export interface User {
  username: string;
  enabled: boolean;
  name: string;
  email: string;
  roles: string[];
}

export interface CreateUserRequest {
  username: string;
  email: string;
  name: string;
  roles: string[];
  sendAccountInfo: boolean;
}

export interface CreateUserDto {
  username: string;
  email: string;
  name: string;
  roles: string[];
  enabled: boolean;
}

export interface UserPageDto {
  users: User[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  lastPage: boolean;
}
