import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import Pagination from '../../components/common/Pagination.vue';

describe('Pagination.vue', () => {
  it('không hiển thị khi totalPages <= 1', () => {
    const wrapper = mount(Pagination, {
      props: {
        currentPage: 0,
        totalPages: 1,
        totalElements: 10,
      },
    });

    expect(wrapper.find('div').exists()).toBe(false);
  });

  it('hiển thị đúng thông tin trang khi totalPages > 1', () => {
    const wrapper = mount(Pagination, {
      props: {
        currentPage: 2,
        totalPages: 5,
        totalElements: 50,
      },
    });

    expect(wrapper.find('div').exists()).toBe(true);
    expect(wrapper.text()).toContain('3 / 5');
  });

  it('vô hiệu hóa nút Trước (Prev) khi đang ở trang đầu tiên (currentPage = 0)', () => {
    const wrapper = mount(Pagination, {
      props: {
        currentPage: 0,
        totalPages: 4,
      },
    });

    const buttons = wrapper.findAll('button');
    expect(buttons[0].attributes('disabled')).toBeDefined();
    expect(buttons[1].attributes('disabled')).toBeUndefined();
  });

  it('vô hiệu hóa nút Kế tiếp (Next) khi đang ở trang cuối cùng (currentPage = totalPages - 1)', () => {
    const wrapper = mount(Pagination, {
      props: {
        currentPage: 3,
        totalPages: 4,
      },
    });

    const buttons = wrapper.findAll('button');
    expect(buttons[0].attributes('disabled')).toBeUndefined();
    expect(buttons[1].attributes('disabled')).toBeDefined();
  });

  it('phát sự kiện "page-change" khi bấm nút Trước hoặc Kế tiếp', async () => {
    const wrapper = mount(Pagination, {
      props: {
        currentPage: 1,
        totalPages: 3,
      },
    });

    const buttons = wrapper.findAll('button');
    // Click prev (page 0)
    await buttons[0].trigger('click');
    expect(wrapper.emitted('page-change')).toBeTruthy();
    expect(wrapper.emitted('page-change')![0]).toEqual([0]);

    // Click next (page 2)
    await buttons[1].trigger('click');
    expect(wrapper.emitted('page-change')![1]).toEqual([2]);
  });
});
