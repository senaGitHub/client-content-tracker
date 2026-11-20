import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: AppLayout,
      children: [
        {
          path: '',
          name: 'overview',
          component: () => import('@/views/OverviewView.vue'),
          meta: { title: 'Genel Bakış' },
        },
        {
          path: 'clients',
          name: 'clients',
          component: () => import('@/views/ClientsView.vue'),
          meta: { title: 'Markalar' },
        },
        {
          // Escaped twice: in a JS string '\d' is just 'd', so the route would only match the letter d.
          path: 'clients/:id(\\d+)',
          name: 'client-detail',
          component: () => import('@/views/ClientDetailView.vue'),
          props: (route) => ({ id: Number(route.params.id) }),
          meta: { title: 'Marka' },
        },
        {
          path: 'content',
          name: 'content',
          component: () => import('@/views/ContentView.vue'),
          meta: { title: 'İçerikler' },
        },
        {
          path: 'approvals',
          name: 'approvals',
          component: () => import('@/views/ApprovalsView.vue'),
          meta: { title: 'Onay Süreci' },
        },
        {
          path: ':pathMatch(.*)*',
          name: 'not-found',
          component: () => import('@/views/NotFoundView.vue'),
          meta: { title: 'Sayfa bulunamadı' },
        },
      ],
    },
  ],
  scrollBehavior: (_to, _from, saved) => saved ?? { top: 0 },
})

router.afterEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} · Harbour` : 'Harbour'
})

export default router
