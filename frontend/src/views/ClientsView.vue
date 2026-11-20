<script setup lang="ts">
import { BuildingIcon, CircleAlertIcon, PlusIcon, SearchIcon, SearchXIcon, XIcon } from '@lucide/vue'
import { useDebounceFn } from '@vueuse/core'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clientsApi } from '@/api'
import BrandCard from '@/components/client/BrandCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useAsyncData } from '@/composables/useAsyncData'
import { useClientDialog } from '@/composables/useClientDialog'
import { useNavCounts } from '@/composables/useNavCounts'
import { cn } from '@/lib/utils'

// The API caps a page at 100: one page holds every brand an agency team works with.
const PAGE_SIZE = 100

const route = useRoute()
const router = useRouter()
const { open: openNewClient, onSaved } = useClientDialog()
const navCounts = useNavCounts()

// The search lives in the URL (?q=), so reloads and Back keep it.
const q = computed(() => (typeof route.query.q === 'string' ? route.query.q : ''))

const { data, error, loading, reload } = useAsyncData(
  (signal) => clientsApi.list({ q: q.value.trim() || undefined, size: PAGE_SIZE }, signal),
  { immediate: false, errorToast: 'Markalar yüklenemedi' },
)
watch(q, () => void reload(), { immediate: true })
onSaved(() => void reload())

// Typing updates the box at once; the URL (and the request) follow after a short pause.
const search = ref(q.value)
const commitSearch = useDebounceFn((value: string) => {
  void router.replace({ query: value.trim() ? { q: value.trim() } : {} })
}, 300)
watch(search, (value) => {
  if (value.trim() !== q.value) void commitSearch(value)
})
watch(q, (value) => {
  if (value !== search.value.trim()) search.value = value
})

function clearSearch() {
  search.value = ''
  void router.replace({ query: {} })
}

const description = computed(() => {
  const total = navCounts.value?.brands
  return total === undefined ? 'Müşteri markalarınız ve içerik durumları.' : `${total} müşteri markanız ve içerik durumları.`
})

const showSkeleton = computed(() => loading.value && !data.value)
const isEmpty = computed(() => !!data.value && data.value.totalElements === 0)
</script>

<template>
  <PageHeader title="Markalar" :description="description" class="mb-5">
    <template #actions>
      <Button size="lg" @click="openNewClient">
        <PlusIcon :stroke-width="2.4" aria-hidden="true" />
        Yeni Marka
      </Button>
    </template>
  </PageHeader>

  <!-- Same place and size as the Content page's search (ContentToolbar). -->
  <div role="search" aria-label="Markaları ara" class="mb-5">
    <label
      class="flex h-10 w-full items-center gap-2.5 rounded-lg border border-input bg-card px-3.5 transition-shadow focus-within:border-ring focus-within:ring-3 focus-within:ring-ring/50 sm:w-[260px]"
    >
      <SearchIcon class="size-[17px] shrink-0 text-foreground" aria-hidden="true" />
      <span class="sr-only">Marka ara</span>
      <input
        v-model="search"
        type="search"
        placeholder="Marka ara..."
        class="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground [&::-webkit-search-cancel-button]:hidden"
        @keydown.esc="clearSearch"
      />
      <button
        v-if="search"
        type="button"
        class="-mr-1.5 flex size-6 shrink-0 items-center justify-center rounded-md text-muted-foreground outline-none hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50"
        aria-label="Aramayı temizle"
        @click="clearSearch"
      >
        <XIcon class="size-3.5" />
      </button>
    </label>
  </div>

  <div
    v-if="error && !data"
    role="alert"
    class="flex flex-col items-start gap-3 rounded-card border bg-card p-5 shadow-card sm:flex-row sm:items-center sm:justify-between"
  >
    <p class="flex items-start gap-2 text-sm text-muted-foreground">
      <CircleAlertIcon class="mt-0.5 size-4 shrink-0 text-coral-text" aria-hidden="true" />
      <span><span class="font-medium text-foreground">Markalar yüklenemedi.</span> {{ error }}</span>
    </p>
    <Button variant="outline" :disabled="loading" @click="reload">Tekrar dene</Button>
  </div>

  <EmptyState
    v-else-if="isEmpty && q"
    :icon="SearchXIcon"
    title="Bu aramaya uyan marka yok"
    description="Marka adı, sektör ya da ilgili kişi adıyla arayabilirsiniz."
  >
    <Button variant="outline" @click="clearSearch">Aramayı temizle</Button>
  </EmptyState>

  <EmptyState
    v-else-if="isEmpty"
    :icon="BuildingIcon"
    title="Henüz marka yok"
    description="İlk markanızı ekleyin; içerikleri ona göre planlayabilirsiniz."
  >
    <Button @click="openNewClient">Yeni Marka</Button>
  </EmptyState>

  <div
    v-else
    :class="cn('grid grid-cols-1 gap-[18px] transition-opacity sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4', loading && data && 'opacity-60')"
    :aria-busy="loading"
  >
    <template v-if="showSkeleton">
      <div v-for="n in 4" :key="n" class="overflow-hidden rounded-card border bg-card shadow-card" aria-hidden="true">
        <Skeleton class="h-[140px] rounded-none" />
        <div class="space-y-4 px-[18px] py-4">
          <Skeleton class="h-5 w-1/2 rounded-full" />
          <Skeleton class="h-9 w-3/4" />
        </div>
      </div>
    </template>

    <template v-else>
      <BrandCard v-for="client in data?.items" :key="client.id" :client="client" />
    </template>
  </div>
</template>
