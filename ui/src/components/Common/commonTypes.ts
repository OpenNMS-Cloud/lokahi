export interface ItemStatus {
    title: string;
    status: string;
    statusColor: string;
    statusText: string;
}

export interface ItemPreviewProps {
    loading: boolean;
    title: string;
    itemTitle: string;
    itemSubtitle: string;
    itemStatuses: ItemStatus[];
}