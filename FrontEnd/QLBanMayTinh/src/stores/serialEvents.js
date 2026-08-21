import { reactive } from "vue";

// Dùng làm event bus: PosPanel gọi bump() khi thay đổi serial,
// SerialManager watch bump.count để reload.
export const serialEvents = reactive({ count: 0 });

export const bumpSerialEvent = () => { serialEvents.count++; };
