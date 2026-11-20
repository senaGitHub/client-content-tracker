<script setup lang="ts">
import { ArrowLeftIcon, CircleAlertIcon, MailIcon, PlusIcon, SearchXIcon } from '@lucide/vue'
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { LocationQueryRaw } from 'vue-router'
import { ApiError, clientsApi } from '@/api'
import type { ContentStatus } from '@/api'
import BrandCover from '@/components/client/BrandCover.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ContentList from '@/components/content/list/ContentList.vue'
import ThemeToggle from '@/components/layout/ThemeToggle.vue'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useAsyncData } from '@/composables/useAsyncData'
import { useContentDialog } from '@/composables/useContentDialog'
import { STATUS_LABELS, STATUS_ORDER } from '@/lib/content'
import { formatSince } from '@/lib/format'
import { cn } from '@/lib/utils'

const props = defineProps<{ id: number }>()

const route = useRoute()
const router = useRouter()
const { openNew, onSaved } = useContentDialog()

// ---- Brand -----------------------------------------------------------------------------------

/** null = the brand doesn't exist (404), shown as its own empty state. */
const { data: client, error, loading, reload } = useAsyncData(
  async (signal) => {
    try {
      return await clientsApi.get(props.id, signal)
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) return null
      throw e
    }
  },
  { immediate: false, errorToast: 'Marka yüklenemedi' },
)
watch(() => props.id, () => void reload(), { immediate: true })
// Counts change with every create, edit or status change made in the dialog or sheet.
onSaved(() => void reload())

watch(client, (c) => {
  if (c) document.title = `${c.name} · Harbour`
})

const hasCover = computed(() => !!client.value?.coverImageUrl)

// ---- Status tab and page live in the URL, so reload and Back keep them ------------------------

function first(value: unknown): string | undefined {
  return (Array.isArray(value) ? value[0] : value) ?? undefined
}

const status = computed(() => {
  const s = first(route.query.status) as ContentStatus | undefined
  return s && STATUS_ORDER.includes(s) ? s : undefined
})
const page = computed(() => {
  const p = Number(first(route.query.page))
  return Number.isInteger(p) && p > 1 ? p : 1
})

function update(patch: { status?: ContentStatus; page?: number }, { keepPage = false } = {}) {
  const next = { status: status.value, page: page.value, ...patch }
  if (!keepPage && !('page' in patch)) next.page = 1
  const query: LocationQueryRaw = {}
  if (next.status) query.status = next.status
  if (next.page > 1) query.page = String(next.page)
  void router.replace({ query })
}

const filters = computed(() => ({ clientId: props.id }))

const tabs = computed(() => {
  const c = client.value?.counts
  return [undefined, ...STATUS_ORDER].map((value) => ({
    value,
    label: value ? STATUS_LABELS[value] : 'Tümü',
    count: c ? (value ? { DRAFT: c.draft, IN_REVIEW: c.inReview, PUBLISHED: c.published }[value] : c.total) : undefined,
  }))
})

const stats = computed(() => {
  const c = client.value?.counts
  if (!c) return []
  return [
    { label: 'Taslak', value: c.draft, alert: false },
    { label: 'Onayda', value: c.inReview, alert: false },
    { label: 'Yayında', value: c.published, alert: false },
    // Red only when something is actually overdue; at 0 it is a neutral tile like the others.
    { label: 'Geciken', value: c.overdue, alert: c.overdue > 0 },
  ]
})
</script>

<template>
  <!-- This page has no PageHeader, so the desktop theme toggle sits at the end of the back-link row. -->
  <div class="mb-5 flex items-center justify-between gap-3">
    <RouterLink
      to="/clients"
      class="inline-flex items-center gap-1.5 rounded-md text-sm font-semibold text-coral-text outline-none hover:underline hover:underline-offset-4 focus-visible:ring-3 focus-visible:ring-ring/50"
    >
      <ArrowLeftIcon class="size-4" aria-hidden="true" />
      Markalar
    </RouterLink>
    <ThemeToggle class="hidden lg:inline-flex" />
  </div>

  <!-- Loading -->
  <div v-if="loading && client === undefined" class="flex flex-col gap-5" aria-busy="true">
    <Skeleton class="h-[180px] rounded-dialog" />
    <div class="grid grid-cols-2 gap-3.5 md:grid-cols-4">
      <Skeleton v-for="n in 4" :key="n" class="h-[92px] rounded-card" />
    </div>
    <Skeleton class="h-72 rounded-card" />
  </div>

  <!-- Failed (not a 404) -->
  <div
    v-else-if="error && client === undefined"
    role="alert"
    class="flex flex-col items-start gap-3 rounded-card border bg-card p-5 shadow-card sm:flex-row sm:items-center sm:justify-between"
  >
    <p class="flex items-start gap-2 text-sm text-muted-foreground">
      <CircleAlertIcon class="mt-0.5 size-4 shrink-0 text-coral-text" aria-hidden="true" />
      <span><span class="font-medium text-foreground">Marka yüklenemedi.</span> {{ error }}</span>
    </p>
    <Button variant="outline" :disabled="loading" @click="reload">Tekrar dene</Button>
  </div>

  <!-- 404 -->
  <EmptyState
    v-else-if="client === null"
    :icon="SearchXIcon"
    title="Marka bulunamadı"
    description="Bu adreste bir marka yok. Bağlantı eski ya da hatalı olabilir."
  >
    <Button as-child variant="outline"><RouterLink to="/clients">Markalara dön</RouterLink></Button>
  </EmptyState>

  <div v-else-if="client" class="flex flex-col gap-5">
    <!-- Hero -->
    <section
      :class="cn('relative flex min-h-[180px] items-end overflow-hidden rounded-dialog', hasCover ? 'text-white' : 'text-foreground')"
      aria-labelledby="brand-name"
    >
      <BrandCover
        :name="client.name"
        :url="client.coverImageUrl"
        class="absolute inset-0 size-full"
        initials-class="mt-6 mr-8 ml-auto self-start text-[72px] opacity-70"
      />
      <span
        v-if="hasCover"
        class="absolute inset-0 bg-linear-to-r from-[rgba(20,14,10,0.75)] to-[rgba(20,14,10,0.2)] to-60%"
        aria-hidden="true"
      />
      <div class="relative flex w-full flex-col gap-4 p-6 md:flex-row md:items-end md:justify-between md:px-8 md:pb-7">
        <div class="min-w-0">
          <p :class="cn('text-sm', hasCover ? 'opacity-90' : 'text-secondary-text')">
            {{ client.industry }} · {{ formatSince(client.createdAt) }}
          </p>
          <h1 id="brand-name" class="mt-1 text-hero-title break-words">
            {{ client.name }}
          </h1>
        </div>
        <div class="flex flex-wrap gap-2.5 md:shrink-0">
          <Button as-child variant="outline" size="lg" class="border-0 bg-white/95 text-black/85 hover:bg-white hover:text-black dark:bg-white/95 dark:hover:bg-white">
            <a :href="`mailto:${client.contactEmail}`" :title="client.contactEmail" :aria-label="`${client.contactName} kişisine e-posta gönder`">
              <MailIcon :stroke-width="1.8" aria-hidden="true" />
              {{ client.contactName }}
            </a>
          </Button>
          <Button size="lg" @click="openNew({ clientId: client.id })">
            <PlusIcon :stroke-width="2.4" aria-hidden="true" />
            Bu markaya içerik ekle
          </Button>
        </div>
      </div>
    </section>

    <!-- Stats -->
    <div class="grid grid-cols-2 gap-3.5 md:grid-cols-4">
      <div
        v-for="stat in stats"
        :key="stat.label"
        :class="cn('rounded-card border p-[18px] shadow-card', stat.alert ? 'border-overdue-border bg-overdue-soft' : 'bg-card')"
      >
        <p :class="cn('text-[13px]', stat.alert ? 'font-semibold text-overdue-text' : 'text-muted-foreground')">{{ stat.label }}</p>
        <p :class="cn('mt-1 text-kpi tabular-nums', stat.alert && 'text-overdue-text')">{{ stat.value }}</p>
      </div>
    </div>

    <!-- Contents -->
    <section aria-labelledby="brand-contents" class="flex flex-col gap-3.5">
      <div class="flex flex-wrap items-center gap-x-3.5 gap-y-2">
        <h2 id="brand-contents" class="text-section-title">İçerikler</h2>
        <div role="group" aria-label="Durum" class="no-scrollbar -mx-1 flex max-w-full gap-1 overflow-x-auto px-1 py-1">
          <button
            v-for="tab in tabs"
            :key="tab.label"
            type="button"
            :aria-pressed="status === tab.value"
            :class="cn(
              'shrink-0 rounded-lg px-3.5 py-2 text-sm whitespace-nowrap outline-none transition-colors focus-visible:ring-3 focus-visible:ring-ring/50',
              status === tab.value ? 'bg-coral-soft font-semibold text-coral-text' : 'font-medium text-secondary-text hover:bg-muted',
            )"
            @click="update({ status: tab.value })"
          >
            {{ tab.label }}<span v-if="tab.count !== undefined" class="tabular-nums"> ({{ tab.count }})</span>
          </button>
        </div>
      </div>

      <ContentList
        brand
        :filters="filters"
        :status="status"
        :page="page"
        :has-filters="!!status"
        @update="(patch, options) => update(patch, options)"
        @clear="update({ status: undefined })"
        @changed="reload()"
      />
    </section>
  </div>
</template>
