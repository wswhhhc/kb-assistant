import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginPage.vue')
  },
  {
    path: '/',
    component: () => import('@/components/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardPage.vue')
      },
      {
        path: 'users',
        name: 'UserManage',
        component: () => import('@/views/user/UserManagePage.vue')
      },
      {
        path: 'knowledge-bases',
        name: 'KnowledgeBaseList',
        component: () => import('@/views/knowledgeBase/KnowledgeBaseList.vue')
      },
      {
        path: 'knowledge-bases/:id',
        name: 'KnowledgeBaseDetail',
        component: () => import('@/views/knowledgeBase/KnowledgeBaseDetail.vue')
      },
      {
        path: 'documents/:kbId',
        name: 'DocumentManage',
        component: () => import('@/views/document/DocumentManage.vue')
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('@/views/chat/ChatPage.vue')
      },
      {
        path: 'feedback',
        name: 'FeedbackRecords',
        component: () => import('@/views/feedback/FeedbackRecords.vue')
      },
      {
        path: 'failed-questions',
        name: 'FailedQuestionList',
        component: () => import('@/views/failedQuestion/FailedQuestionList.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.name !== 'Login' && !token) {
    next({ name: 'Login' })
  } else {
    next()
  }
})

export default router
