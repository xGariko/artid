import type { Tag } from './schemas';

export interface SidebarButtons {
	icon: string;
	label: string;
	value: string;
	count: number;
}

// export interface SidebarTags {
// 	tag: Tag;
// }

export interface SidebarButtonGroup {
	label: string;
	buttons?: SidebarButtons[];
	tags?: Tag[];
	sidebarAction?: SidebarAction;
}

export interface SidebarAction {
	label: string;
	icon: string;
	callback: () => void;
	type: 'button' | 'link' | 'tag';
}
