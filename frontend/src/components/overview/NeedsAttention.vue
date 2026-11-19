<script setup lang="ts">
import type { NeedsAttention } from '@/api'
import PageSection from '@/components/common/PageSection.vue'
import { contentLink } from '@/lib/content'
import AttentionGroup from './AttentionGroup.vue'

defineProps<{ data?: NeedsAttention; loading?: boolean }>()
</script>

<template>
  <PageSection title="Needs attention" description="Late work, this week's deadlines and items waiting for sign-off.">
    <div class="grid gap-3 lg:grid-cols-3">
      <AttentionGroup
        title="Overdue"
        tone="overdue"
        :items="data?.overdue"
        :loading="loading"
        empty-text="Nothing overdue."
        :view-all="contentLink({ overdue: true })"
      />
      <AttentionGroup
        title="Due this week"
        :items="data?.dueThisWeek"
        :loading="loading"
        empty-text="Nothing due this week."
        :view-all="contentLink({ dueThisWeek: true })"
      />
      <AttentionGroup
        title="Awaiting approval"
        :items="data?.awaitingApproval"
        :loading="loading"
        :show-status="false"
        empty-text="Nothing waiting for approval."
        :view-all="contentLink({ status: 'IN_REVIEW' })"
      />
    </div>
  </PageSection>
</template>
