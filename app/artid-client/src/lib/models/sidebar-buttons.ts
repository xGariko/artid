export interface SidebarButtons {
	icon: string;
	label: string;
	value: string;
	count: number;
}

export interface SidebarButtonGroup { label: string; buttons: SidebarButtons[], type: 'button' | 'link', sidebarAction?: SidebarAction }

export interface SidebarAction {
	label: string;
	icon: string;
	callback: ()=>void;
	type: 'button' | 'link';
}