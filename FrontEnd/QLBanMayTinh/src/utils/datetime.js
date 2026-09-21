// Lấy thời gian ISO cục bộ định dạng YYYY-MM-DDTHH:mm:ss
export const nowLocalIso = (date = new Date()) =>
  new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 19);
