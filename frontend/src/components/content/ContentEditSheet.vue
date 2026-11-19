<script setup lang="ts">
import { LoaderCircleIcon, XIcon } from '@lucide/vue'
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { toast } from 'vue-sonner'
import { contentsApi } from '@/api'
import type { ActivityResponse, ContentResponse, ContentStatus } from '@/api'
import OverdueChip from '@/components/content/OverdueChip.vue'
import StatusBadge from '@/components/content/StatusBadge.vue'
import TypePicker from '@/components/content/form/TypePicker.vue'
import { Button } from '@/components/ui/button'
import type { ButtonVariants } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '@/components/ui/dialog'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { useClientOptions } from '@/composables/useClientOptions'
import { notifyContentSaved, useContentDialog } from '@/composables/useContentDialog'
import { CHANNEL_SUGGESTIONS, useContentForm } from '@/composables/useContentForm'
import { refreshNavCounts } from '@/composables/useNavCounts'
import { asPublishing, useStatusChange } from '@/composables/useStatusChange'
import type { PlacePhase } from '@/composables/useStatusChange'
import { STATUS_LABELS, STATUS_STYLES, TYPE_LABELS } from '@/lib/content'
import { errorMessage } from '@/lib/errors'
import { formatDateTime, formatUpdated } from '@/lib/format'

const { state } = useContentDialog()
const { clients } = useClientOptions()
const route = useRoute()
const { form, errors, reset, isDirty, validate, focusFirstError, applyServerErrors, body, id, describedBy, onKeydown } =
  useContentForm('edit-content')

const item = computed(() => state.content)
const saving = ref(false)

// The form is filled when the sheet opens (or shows another item), never when only the status
// changes, so unsaved edits survive a status change.
watch(
  () => [state.editOpen, state.content?.id] as const,
  ([open, contentId], previous) => {
    if (!open || !state.content) return
    if (previous && previous[0] && previous[1] === contentId) return
    reset(state.content)
    void loadHistory(state.content.id)
  },
)

// ---- History ----------------------------------------------------------------------------------

const history = ref<ActivityResponse[]>()
const historyError = ref<string | null>(null)
const historyLoading = ref(false)
let historyController: AbortController | null = null

async function loadHistory(contentId: number) {
  historyController?.abort()
  const current = new AbortController()
  historyController = current
  historyLoading.value = true
  historyError.value = null
  if (history.value && state.content?.id !== contentId) history.value = undefined
  try {
    history.value = await contentsApi.activity(contentId, current.signal)
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') return
    historyError.value = errorMessage(e)
  } finally {
    if (historyController === current) historyLoading.value = false
  }
}

function historyText(entry: ActivityResponse): string {
  if (entry.action === 'STATUS_CHANGED' && entry.fromStatus && entry.toStatus) {
    return `${STATUS_LABELS[entry.fromStatus]} → ${STATUS_LABELS[entry.toStatus]}`
  }
  return entry.action === 'CREATED' ? 'Oluşturuldu' : 'Düzenlendi'
}

// ---- Status (independent of the form: applied at once, not on save) ---------------------------

function place(updated: ContentResponse, phase: PlacePhase) {
  if (state.content?.id === updated.id) state.content = updated
  if (phase === 'confirmed') {
    // Only server-confirmed changes reach the pages behind the sheet.
    notifyContentSaved(updated)
    if (state.content?.id === updated.id) void loadHistory(updated.id)
  }
}
const status = useStatusChange(place)

// Leaving the page sends a publish that is still waiting (the sheet itself never unmounts).
watch(() => route.path, () => status.flush())

const isPublishing = computed(() => !!item.value && item.value.status === 'PUBLISHED' && status.isPending(item.value.id))
const isBusy = computed(() => !!item.value && status.isPending(item.value.id))

interface StatusAction {
  target: ContentStatus
  label: string
  variant: ButtonVariants['variant']
}

const actions = computed<StatusAction[]>(() => {
  const current = item.value
  if (!current) return []
  const order: ContentStatus[] = ['PUBLISHED', 'IN_REVIEW', 'DRAFT']
  return order
    .filter((s) => current.allowedTransitions.includes(s))
    .map((target) => {
      if (target === 'PUBLISHED') return { target, label: 'Onayla ve yayına al', variant: 'approveSolid' as const }
      if (target === 'IN_REVIEW') return { target, label: 'Onaya gönder', variant: 'default' as const }
      return { target, label: current.status === 'IN_REVIEW' ? 'Revizyona gönder' : 'Taslağa geri gönder', variant: 'outline' as const }
    })
})

// ---- Save ---------------------------------------------------------------------------------------

const dirty = computed(() => isDirty())

async function submit() {
  const current = item.value
  if (!current || saving.value) return
  if (!validate()) return focusFirstError()
  saving.value = true
  try {
    // The body has no status, so this can never undo a status change made in the sheet.
    const saved = await contentsApi.update(current.id, body())
    // A publish that is still waiting is not on the server yet; keep showing it as published.
    state.content = status.isPending(saved.id) ? asPublishing(saved) : saved
    toast.success('Değişiklikler kaydedildi', { description: saved.title })
    notifyContentSaved(saved)
    void refreshNavCounts()
    state.editOpen = false
  } catch (e) {
    if (!applyServerErrors(e)) toast.error('Değişiklikler kaydedilemedi', { description: errorMessage(e) })
  } finally {
    saving.value = false
  }
}

// ---- Closing: unsaved edits are only dropped after a confirmation ---------------------------

const confirmOpen = ref(false)

/** Every way of closing (Esc, outside click, Vazgeç, X) comes through here; a save closes directly. */
function requestClose() {
  if (saving.value) return
  if (dirty.value) confirmOpen.value = true
  else state.editOpen = false
}

function onOpenChange(open: boolean) {
  if (open) state.editOpen = true
  else requestClose()
}

function discardAndClose() {
  confirmOpen.value = false
  state.editOpen = false
}

const meta = computed(() => {
  const current = item.value
  if (!current) return ''
  return [current.client.name, TYPE_LABELS[current.type], current.channel].filter(Boolean).join(' · ')
})

const labelClass = 'text-sm font-semibold'
const optional = 'font-normal text-muted-foreground'
</script>

<template>
  <Sheet :open="state.editOpen" @update:open="onOpenChange">
    <SheetContent
      side="right"
      :show-close-button="false"
      class="gap-0 p-0 data-[side=right]:w-full data-[side=right]:sm:max-w-[500px]"
    >
      <template v-if="item">
        <!-- Brand cover -->
        <div class="relative h-[110px] shrink-0 bg-column">
          <img
            v-if="item.client.coverImageUrl"
            :src="item.client.coverImageUrl"
            alt=""
            class="absolute inset-0 size-full object-cover"
          />
          <Button
            variant="secondary"
            size="icon"
            class="absolute top-3.5 right-3.5 size-[34px] bg-card/90 text-foreground hover:bg-card"
            aria-label="Kapat"
            @click="requestClose"
          >
            <XIcon />
          </Button>
        </div>

        <div class="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto px-5 py-5 sm:px-[26px]">
          <div>
            <SheetDescription class="text-[13px] text-muted-foreground">{{ meta }}</SheetDescription>
            <SheetTitle class="mt-1 text-[22px] leading-tight font-extrabold tracking-[-0.01em] break-words">
              {{ item.title }}
            </SheetTitle>
            <div class="mt-2.5 flex items-center gap-2">
              <StatusBadge :status="item.status" />
              <OverdueChip v-if="item.overdue" :due-date="item.dueDate" />
            </div>
          </div>

          <!-- Status actions: only the moves the rule allows; applied at once, separate from Save. -->
          <section aria-labelledby="edit-status-heading" class="rounded-2xl border bg-background p-3.5">
            <h3 id="edit-status-heading" class="mb-2.5 text-[13px] font-bold">Durumu ilerlet</h3>
            <p v-if="isPublishing" class="flex items-center gap-2 text-sm text-secondary-text" aria-live="polite">
              <LoaderCircleIcon class="size-4 animate-spin text-approve" aria-hidden="true" />
              Yayına alınıyor… Bildirimdeki “Geri al” ile iptal edebilirsiniz.
            </p>
            <p v-else-if="actions.length === 0" class="text-sm text-secondary-text">
              Yayındaki içerik son durumdadır; durumu değiştirilemez.
            </p>
            <div v-else class="flex flex-col gap-2 sm:flex-row">
              <Button
                v-for="action in actions"
                :key="action.target"
                type="button"
                :variant="action.variant"
                class="sm:flex-1"
                :disabled="isBusy"
                @click="status.move(item, action.target)"
              >
                {{ action.label }}
              </Button>
            </div>
            <p class="mt-2 text-xs text-muted-foreground">Yalnızca kurala uygun geçişler gösterilir.</p>
          </section>

          <form
            id="edit-content-form"
            class="grid gap-4"
            novalidate
            @submit.prevent="submit"
            @keydown="(e: KeyboardEvent) => onKeydown(e, submit)"
          >
            <div class="grid gap-1.5">
              <Label :for="id('title')" :class="labelClass">Başlık</Label>
              <Input
                :id="id('title')"
                v-model="form.title"
                maxlength="200"
                autocomplete="off"
                :aria-invalid="!!errors.title"
                :aria-describedby="describedBy('title')"
              />
              <p v-if="errors.title" :id="`${id('title')}-error`" class="text-xs font-medium text-destructive">{{ errors.title }}</p>
            </div>

            <div class="grid gap-3 sm:grid-cols-2">
              <div class="grid min-w-0 gap-1.5">
                <Label :for="id('clientId')" :class="labelClass">Marka</Label>
                <Select v-model="form.clientId">
                  <SelectTrigger
                    :id="id('clientId')"
                    class="w-full"
                    :aria-invalid="!!errors.clientId"
                    :aria-describedby="describedBy('clientId')"
                  >
                    <SelectValue placeholder="Marka seçin" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem v-for="client in clients" :key="client.id" :value="String(client.id)">{{ client.name }}</SelectItem>
                  </SelectContent>
                </Select>
                <p v-if="errors.clientId" :id="`${id('clientId')}-error`" class="text-xs font-medium text-destructive">{{ errors.clientId }}</p>
              </div>
              <div class="grid min-w-0 gap-1.5">
                <Label :for="id('dueDate')" :class="labelClass">Teslim tarihi</Label>
                <Input
                  :id="id('dueDate')"
                  v-model="form.dueDate"
                  type="date"
                  :aria-invalid="!!errors.dueDate"
                  :aria-describedby="describedBy('dueDate')"
                />
                <p v-if="errors.dueDate" :id="`${id('dueDate')}-error`" class="text-xs font-medium text-destructive">{{ errors.dueDate }}</p>
              </div>
            </div>

            <div class="grid gap-1.5">
              <span :class="labelClass" class="leading-none">Tür</span>
              <TypePicker :id="id('type')" v-model="form.type" :invalid="!!errors.type" :described-by="describedBy('type')" />
              <p v-if="errors.type" :id="`${id('type')}-error`" class="text-xs font-medium text-destructive">{{ errors.type }}</p>
            </div>

            <div class="grid gap-1.5">
              <Label :for="id('channel')" :class="labelClass">Kanal <span :class="optional">(isteğe bağlı)</span></Label>
              <Input
                :id="id('channel')"
                v-model="form.channel"
                maxlength="60"
                list="edit-content-channels"
                autocomplete="off"
                placeholder="ör. Instagram"
                :aria-invalid="!!errors.channel"
                :aria-describedby="describedBy('channel')"
              />
              <datalist id="edit-content-channels">
                <option v-for="channel in CHANNEL_SUGGESTIONS" :key="channel" :value="channel" />
              </datalist>
              <p v-if="errors.channel" :id="`${id('channel')}-error`" class="text-xs font-medium text-destructive">{{ errors.channel }}</p>
            </div>

            <div class="grid gap-1.5">
              <Label :for="id('notes')" :class="labelClass">Notlar <span :class="optional">(isteğe bağlı)</span></Label>
              <Textarea
                :id="id('notes')"
                v-model="form.notes"
                rows="3"
                maxlength="5000"
                :aria-invalid="!!errors.notes"
                :aria-describedby="describedBy('notes')"
              />
              <p v-if="errors.notes" :id="`${id('notes')}-error`" class="text-xs font-medium text-destructive">{{ errors.notes }}</p>
            </div>
          </form>

          <section aria-labelledby="edit-history-heading">
            <h3 id="edit-history-heading" class="mb-2 text-[13px] font-bold">Geçmiş</h3>
            <div v-if="!history && historyLoading" class="flex flex-col gap-2" aria-hidden="true">
              <Skeleton class="h-3.5 w-40" />
              <Skeleton class="h-3.5 w-32" />
            </div>
            <p v-else-if="historyError && !history" class="text-[13px] text-muted-foreground">
              Geçmiş yüklenemedi.
              <button
                type="button"
                class="font-semibold text-coral-text outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring/50"
                @click="loadHistory(item.id)"
              >
                Tekrar dene
              </button>
            </p>
            <ol v-else-if="history" class="flex flex-col gap-2 text-[13px]">
              <li v-for="entry in history" :key="entry.id">
                <span
                  :class="entry.action === 'STATUS_CHANGED' && entry.toStatus ? ['font-semibold', STATUS_STYLES[entry.toStatus].text] : ''"
                >{{ historyText(entry) }}</span>
                <span class="text-muted-foreground">
                  ·
                  <time :datetime="entry.createdAt" :title="formatDateTime(entry.createdAt)">{{ formatUpdated(entry.createdAt) }}</time>
                </span>
              </li>
            </ol>
          </section>
        </div>

        <div class="flex shrink-0 justify-end gap-2.5 border-t border-divider bg-background px-5 py-3.5 sm:px-[26px]">
          <Button type="button" variant="outline" :disabled="saving" @click="requestClose">Vazgeç</Button>
          <Button type="submit" form="edit-content-form" :disabled="saving || !dirty">
            {{ saving ? 'Kaydediliyor…' : 'Değişiklikleri kaydet' }}
          </Button>
        </div>
      </template>
    </SheetContent>
  </Sheet>

  <!-- Asked before unsaved edits are thrown away. Esc / outside click here means "keep editing". -->
  <Dialog v-model:open="confirmOpen">
    <DialogContent role="alertdialog" :show-close-button="false" class="gap-0 p-0 sm:max-w-[420px]">
      <div class="px-6 pt-6 pb-5">
        <DialogTitle class="text-lg font-extrabold">Kaydedilmemiş değişiklikler silinsin mi?</DialogTitle>
        <DialogDescription class="mt-1.5 text-sm text-muted-foreground">
          Bu içerikte kaydetmediğiniz değişiklikler var. Kapatırsanız kaybolacak.
        </DialogDescription>
      </div>
      <div class="flex flex-col-reverse gap-2.5 border-t border-divider bg-background px-6 py-4 sm:flex-row sm:justify-end">
        <Button type="button" variant="outline" @click="confirmOpen = false">Düzenlemeye dön</Button>
        <Button type="button" @click="discardAndClose">Değişiklikleri sil</Button>
      </div>
    </DialogContent>
  </Dialog>
</template>
