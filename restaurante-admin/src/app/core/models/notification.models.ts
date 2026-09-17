export interface Notification {
  id: number;
  type: string;
  title: string;
  message: string;
  entity: string;
  entityId: string;
  priority: string;
  read: boolean;
  readAt: string | null;
  createdAt: string;
}

export interface NotificationCount {
  unreadCount: number;
}
