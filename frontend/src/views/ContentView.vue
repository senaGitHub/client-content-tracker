<script setup lang="ts">
import { KanbanIcon, ListIcon, PlusIcon } from '@lucide/vue'
import ContentBoard from '@/components/content/board/ContentBoard.vue'
import ContentToolbar from '@/components/content/ContentToolbar.vue'
import ContentList from '@/components/content/list/ContentList.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import { Button } from '@/components/ui/button'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useContentDialog } from '@/composables/useContentDialog'
import { useContentQuery } from '@/composables/useContentQuery'
import type { ContentView } from '@/composables/useContentQuery'

const { state, filters, hasFilters, update, clearFilters } = useContentQuery()
const { openNew } = useContentDialog()

function onViewChange(value: string | number) {
  // Status is a list filter only; the board shows every status as a column.
  update({ view: value as ContentView, status: undefined })
}
</script>

<template>
  <PageHeader title="Content" description="Every post and page, from draft to published.">
    <template #actions>
      <Button class="h-9" @click="openNew({ clientId: state.clientId })">
        <PlusIcon />
        New content
      </Button>
    </template>
  </PageHeader>

  <Tabs :model-value="state.view" class="gap-5" @update:model-value="onViewChange">
    <div class="flex flex-col gap-4">
      <TabsList class="h-9 w-full rounded-lg p-0.5 sm:w-fit" aria-label="View">
        <TabsTrigger value="board" class="h-full px-3 data-active:border-border data-active:bg-card">
          <KanbanIcon />
          Board
        </TabsTrigger>
        <TabsTrigger value="list" class="h-full px-3 data-active:border-border data-active:bg-card">
          <ListIcon />
          List
        </TabsTrigger>
      </TabsList>

      <ContentToolbar
        :query="state"
        :show-status="state.view === 'list'"
        :has-filters="hasFilters"
        @update="(patch) => update(patch)"
        @clear="clearFilters"
      />
    </div>

    <TabsContent value="board">
      <ContentBoard :filters="filters" :has-filters="hasFilters" @clear="clearFilters" />
    </TabsContent>
    <TabsContent value="list">
      <ContentList
        :filters="filters"
        :status="state.status"
        :page="state.page"
        :sort="state.sort"
        :has-filters="hasFilters"
        @update="(patch, options) => update(patch, options)"
        @clear="clearFilters"
      />
    </TabsContent>
  </Tabs>
</template>
