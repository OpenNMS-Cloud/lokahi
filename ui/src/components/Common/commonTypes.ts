export interface ItemStatus {
    title: string;
    status: string;
    statusColor: string;
    statusText: string;
}

export interface ItemPreviewProps {
    loading: boolean;
    loadingCopy: string;
    title: string;
    itemTitle: string;
    itemSubtitle: string;
    itemStatuses: ItemStatus[];
    bottomCopy: string;
}

export enum BadgeTypes {
    info = 'indeterminate',
    error = 'critical',
    success = 'normal'
}