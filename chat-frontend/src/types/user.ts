// ==============================
// User Types
// ==============================

export interface User {
  id: number;
  fullName: string;
  phone: string;
  email?: string;
  dateOfBirth?: string;
  gender?: string;
  address?: string;
}

export interface UserProfile {
  fullName: string;
  dateOfBirth: string;
  gender: string;
  address: string;
}

export interface UserSearchResult {
  id: number;
  fullName: string;
  phone: string;
}
