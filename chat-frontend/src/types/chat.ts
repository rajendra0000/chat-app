// ==============================
// Chat / Group Management Types
// ==============================

export type MemberRole = "ADMIN" | "MEMBER";

export interface ChatMember {
  id?: number;
  chatId?: number;
  userId: number;
  name: string;
  role: MemberRole;
  blocked: boolean;
  phone?: string;
}
