import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import i18n from '@/lib/i18n';
import type {
  CurrentUserVO,
  LexicalRagAnswerVO,
  LexicalRagConversationDetailVO,
  LexicalRagConversationSummaryVO,
  PageResult,
} from '@/lib/contracts';
import { useAuthStore, useUIStore } from '@/store';
import { AppLayout, Sidebar } from './index';

const listLexicalRagConversations = vi.fn();
const getLexicalRagConversation = vi.fn();
const queryLexicalRag = vi.fn();

vi.mock('./NotificationBell', () => ({
  NotificationBell: () => <div data-testid="notification-bell" />,
}));

vi.mock('@/lib/services', () => ({
  aiService: {
    listLexicalRagConversations: (...args: unknown[]) => listLexicalRagConversations(...args),
    getLexicalRagConversation: (...args: unknown[]) => getLexicalRagConversation(...args),
    queryLexicalRag: (...args: unknown[]) => queryLexicalRag(...args),
  },
}));

const originalAuthState = useAuthStore.getState();
const originalUiState = useUIStore.getState();

const multiWorkspaceUser: CurrentUserVO = {
  id: 21,
  username: 'admin.teacher',
  email: 'admin.teacher@example.com',
  displayName: 'Admin Teacher',
  primaryRole: 'ADMIN',
  roles: ['ADMIN', 'TEACHER', 'STUDENT'],
  capabilities: ['ADMIN_CONSOLE', 'TEACHING_WORKSPACE', 'STUDENT_WORKSPACE'],
  studentProfile: null,
  teacherProfile: null,
};

const teacherOnlyUser: CurrentUserVO = {
  ...multiWorkspaceUser,
  id: 22,
  username: 'teacher.only',
  primaryRole: 'TEACHER',
  roles: ['TEACHER'],
  capabilities: ['TEACHING_WORKSPACE'],
};

const studentUser: CurrentUserVO = {
  id: 31,
  username: 'student.demo',
  email: 'student.demo@example.com',
  displayName: 'Student Demo',
  primaryRole: 'STUDENT',
  roles: ['STUDENT'],
  capabilities: ['STUDENT_WORKSPACE'],
  studentProfile: null,
  teacherProfile: null,
};

const LocationProbe: React.FC = () => {
  const location = useLocation();
  return <div data-testid="location-probe">{location.pathname}</div>;
};

function renderWithShell() {
  const client = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/dashboard" element={<div>dashboard-page</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

function assistantPayload(overrides?: Partial<LexicalRagAnswerVO>): LexicalRagAnswerVO {
  return {
    requestId: 'req-1',
    conversationId: 'conv-1',
    generationSource: 'AI',
    model: 'stub-model',
    latencyMs: 12,
    grounded: true,
    answer: 'coin usually means money, while French coin often means corner [C1]',
    explanation: 'Use context instead of surface similarity [C1]',
    recommendedActions: ['Compare the core senses.', 'Check whether the examples can be swapped.'],
    confidence: 0.86,
    citationIds: ['C1'],
    citations: [
      {
        citationId: 'C1',
        sourceType: 'LEXICAL_PAIR',
        sourceId: '1001',
        title: 'coin / coin',
        snippet: 'False friend pair guidance',
        score: 0.91,
      },
    ],
    contextChunks: [
      {
        citationId: 'C1',
        sourceType: 'LEXICAL_PAIR',
        sourceId: '1001',
        title: 'coin / coin',
        content: 'English coin means money while French coin often means corner.',
        snippet: 'English coin means money while French coin often means corner.',
        score: 0.91,
        metadata: { chunkKind: 'LEXICAL_PAIR' },
      },
    ],
    fallbackReason: null,
    ...overrides,
  };
}

describe('Topbar mobile more menu', () => {
  beforeEach(async () => {
    window.localStorage.clear();
    vi.stubGlobal(
      'matchMedia',
      vi.fn().mockImplementation(() => ({
        matches: false,
        media: '',
        onchange: null,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        addListener: vi.fn(),
        removeListener: vi.fn(),
        dispatchEvent: vi.fn(),
      }))
    );
    useAuthStore.setState({
      ...useAuthStore.getState(),
      user: studentUser,
    });
    useUIStore.setState({
      ...useUIStore.getState(),
      locale: 'zh-CN',
      isDarkMode: false,
      isSidebarCollapsed: false,
      activeWorkspace: 'STUDENT_WORKSPACE',
      preferredWorkspaceByUser: {},
    });
    await i18n.changeLanguage('zh-CN');
  });

  afterEach(() => {
    useAuthStore.setState(originalAuthState);
    useUIStore.setState(originalUiState);
    window.localStorage.clear();
    vi.unstubAllGlobals();
    vi.clearAllMocks();
  });

  it('exposes locale and theme controls in the mobile more menu', async () => {
    renderWithShell();

    fireEvent.click(screen.getByRole('button', { name: '更多选项' }));
    fireEvent.click(screen.getByRole('menuitem', { name: 'English' }));

    await waitFor(() => {
      expect(useUIStore.getState().locale).toBe('en-US');
    });

    // AppLayout alone does not bind store locale → i18n; labels stay zh-CN in this unit test.
    fireEvent.click(screen.getByRole('button', { name: '更多选项' }));
    fireEvent.click(screen.getByRole('menuitem', { name: '切换到深色模式' }));

    await waitFor(() => {
      expect(useUIStore.getState().isDarkMode).toBe(true);
    });
  });
});

describe('Sidebar workspace navigation', () => {
  beforeEach(async () => {
    window.localStorage.clear();
    useAuthStore.setState({
      ...useAuthStore.getState(),
      user: multiWorkspaceUser,
    });
    useUIStore.setState({
      ...useUIStore.getState(),
      locale: 'zh-CN',
      isSidebarCollapsed: false,
      activeWorkspace: 'ADMIN_CONSOLE',
      preferredWorkspaceByUser: {},
    });
    await i18n.changeLanguage('zh-CN');
  });

  afterEach(() => {
    useAuthStore.setState(originalAuthState);
    useUIStore.setState(originalUiState);
    window.localStorage.clear();
    vi.clearAllMocks();
  });

  it('shows only the current workspace navigation and switches route/context together', async () => {
    render(
      <MemoryRouter initialEntries={['/admin/users']}>
        <Sidebar />
        <LocationProbe />
      </MemoryRouter>
    );

    expect(screen.getByText('用户管理')).toBeInTheDocument();
    expect(screen.queryByText('教师工作台')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '教师' }));

    await waitFor(() => {
      expect(screen.getByTestId('location-probe')).toHaveTextContent('/teacher/workspace');
    });

    expect(screen.getByText('教师工作台')).toBeInTheDocument();
    expect(screen.queryByText('用户管理')).not.toBeInTheDocument();
    expect(useUIStore.getState().activeWorkspace).toBe('TEACHING_WORKSPACE');
  });

  it('hides the workspace switcher for single-workspace users', () => {
    useAuthStore.setState({
      ...useAuthStore.getState(),
      user: teacherOnlyUser,
    });
    useUIStore.setState({
      ...useUIStore.getState(),
      activeWorkspace: 'TEACHING_WORKSPACE',
      preferredWorkspaceByUser: {},
    });

    render(
      <MemoryRouter initialEntries={['/teacher/workspace']}>
        <Sidebar />
      </MemoryRouter>
    );

    expect(screen.queryByText('切换工作空间')).not.toBeInTheDocument();
  });
});

describe('Assistant drawer conversation flow', () => {
  beforeEach(async () => {
    window.localStorage.clear();
    vi.stubGlobal(
      'matchMedia',
      vi.fn().mockImplementation(() => ({
        matches: false,
        media: '',
        onchange: null,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        addListener: vi.fn(),
        removeListener: vi.fn(),
        dispatchEvent: vi.fn(),
      }))
    );
    useAuthStore.setState({
      ...useAuthStore.getState(),
      user: studentUser,
    });
    useUIStore.setState({
      ...useUIStore.getState(),
      locale: 'zh-CN',
      isSidebarCollapsed: false,
      activeWorkspace: 'STUDENT_WORKSPACE',
      preferredWorkspaceByUser: {},
      isAssistantOpen: true,
      assistantDraft: '',
      activeAssistantConversationId: 'conv-1',
    });

    const conversations: PageResult<LexicalRagConversationSummaryVO> = {
      total: 2,
      pageNo: 1,
      pageSize: 20,
      records: [
        { conversationId: 'conv-2', title: '新的误判追问', lastMessageAt: '2026-04-15T11:00:00Z' },
        { conversationId: 'conv-1', title: 'coin / coin', lastMessageAt: '2026-04-15T10:00:00Z' },
      ],
    };
    const conversationDetails: Record<string, LexicalRagConversationDetailVO> = {
      'conv-1': {
        conversationId: 'conv-1',
        title: 'coin / coin',
        scene: 'LEXICAL_RAG_QUERY',
        lastMessageAt: '2026-04-15T10:00:00Z',
        messages: [
          {
            messageId: 1,
            role: 'user',
            content: 'coin / coin 有什么区别？',
            requestId: null,
            assistantPayload: null,
            createdAt: '2026-04-15T10:00:00Z',
          },
          {
            messageId: 2,
            role: 'assistant',
            content: 'coin usually means money...',
            requestId: 'req-1',
            assistantPayload: assistantPayload({ conversationId: 'conv-1' }),
            createdAt: '2026-04-15T10:01:00Z',
          },
        ],
      },
      'conv-2': {
        conversationId: 'conv-2',
        title: '新的误判追问',
        scene: 'LEXICAL_RAG_QUERY',
        lastMessageAt: '2026-04-15T11:00:00Z',
        messages: [
          {
            messageId: 3,
            role: 'user',
            content: '那为什么总会误判？',
            requestId: null,
            assistantPayload: null,
            createdAt: '2026-04-15T11:00:00Z',
          },
          {
            messageId: 4,
            role: 'assistant',
            content: 'Because the pair is a classic false friend...',
            requestId: 'req-2',
            assistantPayload: assistantPayload({
              requestId: 'req-2',
              conversationId: 'conv-2',
              answer: 'Because the pair is a classic false friend [C1]',
            }),
            createdAt: '2026-04-15T11:01:00Z',
          },
        ],
      },
    };

    listLexicalRagConversations.mockResolvedValue(conversations);
    getLexicalRagConversation.mockImplementation((conversationId: string) => Promise.resolve(conversationDetails[conversationId]));
    queryLexicalRag.mockResolvedValue(
      assistantPayload({
        requestId: 'req-2',
        conversationId: 'conv-2',
        answer: 'Because the pair is a classic false friend [C1]',
      })
    );

    await i18n.changeLanguage('zh-CN');
  });

  afterEach(() => {
    useAuthStore.setState(originalAuthState);
    useUIStore.setState(originalUiState);
    window.localStorage.clear();
    vi.clearAllMocks();
  });

  it('overlays the assistant drawer without inserting it into the app flex shell', async () => {
    renderWithShell();

    const dialog = await screen.findByRole('dialog', { name: '追问词汇误判原因' });
    expect(dialog).toHaveClass('fixed');
    expect(dialog).toHaveClass('surface-panel');
    // Portal to body so max-w drawer never becomes an in-flow flex sibling.
    expect(dialog.parentElement).toBe(document.body);
    expect(document.body.contains(dialog)).toBe(true);
    expect(dialog.closest('.min-h-screen')).toBeNull();
  });

  it('renders conversation history, starts a new chat, and switches conversations', async () => {
    renderWithShell();

    const conversationLabels = await screen.findAllByText('coin / coin');
    const firstConversationButton = conversationLabels.find((element) => element.closest('button'))?.closest('button') ?? null;
    expect(firstConversationButton).not.toBeNull();
    expect(await screen.findByText('coin usually means money, while French coin often means corner [C1]')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '新对话' }));
    await waitFor(() => {
      expect(useUIStore.getState().activeAssistantConversationId).toBeNull();
      expect(screen.getByText('选择一个已有会话，或新建一个对话开始连续追问。')).toBeInTheDocument();
    });

    fireEvent.change(
      screen.getByPlaceholderText('输入你想追问的误判问题，例如：coin / coin 为什么总被误判？'),
      { target: { value: '为什么总会误判？' } }
    );
    fireEvent.click(screen.getByRole('button', { name: '追问原因' }));

    await waitFor(() => {
      expect(queryLexicalRag).toHaveBeenCalledWith({ query: '为什么总会误判？', conversationId: null });
    });

    await waitFor(() => {
      expect(useUIStore.getState().activeAssistantConversationId).toBe('conv-2');
    });

    fireEvent.click(firstConversationButton as HTMLButtonElement);

    await waitFor(() => {
      expect(screen.getByText('coin usually means money, while French coin often means corner [C1]')).toBeInTheDocument();
    });
  });
});
