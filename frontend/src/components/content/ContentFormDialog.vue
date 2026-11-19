<script setup lang="ts">
import { addDays, format } from 'date-fns'
import { nextTick, reactive, ref, watch } from 'vue'
import { toast } from 'vue-sonner'
import { ApiError, contentsApi } from '@/api'
import { errorMessage, fieldErrors } from '@/lib/errors'
import type { ContentRequest, ContentType } from '@/api'
import StatusBadge from '@/components/content/StatusBadge.vue'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { useClientOptions } from '@/composables/useClientOptions'
import { notifyContentSaved, useContentDialog } from '@/composables/useContentDialog'
import { refreshNavCounts } from '@/composables/useNavCounts'
import { TYPE_LABELS } from '@/lib/content'

type Field = 'clientId' | 'title' | 'type' | 'channel' | 'dueDate' | 'notes'

const { state } = useContentDialog()
const { clients } = useClientOptions()

const CHANNEL_SUGGESTIONS = ['Instagram', 'Facebook', 'LinkedIn', 'TikTok', 'Website', 'Blog', 'Newsletter']
const TYPES = Object.keys(TYPE_LABELS) as ContentType[]
// Order matters: the first invalid field in this order gets focus.
const FIELDS: Field[] = ['clientId', 'title', 'type', 'channel', 'dueDate', 'notes']

const form = reactive({ clientId: '', title: '', type: '', channel: '', dueDate: '', notes: '' })
const errors = reactive<Partial<Record<Field, string>>>({})
const saving = ref(false)

function reset() {
  const c = state.content
  form.clientId = c ? String(c.client.id) : state.clientId ? String(state.clientId) : ''
  form.title = c?.title ?? ''
  form.type = c?.type ?? ''
  form.channel = c?.channel ?? ''
  form.dueDate = c?.dueDate ?? format(addDays(new Date(), 7), 'yyyy-MM-dd')
  form.notes = c?.notes ?? ''
  FIELDS.forEach((f) => delete errors[f])
}

watch(() => state.open, (open) => open && reset())

function validate(): boolean {
  FIELDS.forEach((f) => delete errors[f])
  if (!form.clientId) errors.clientId = 'Client is required.'
  if (!form.title.trim()) errors.title = 'Title is required.'
  if (!form.type) errors.type = 'Type is required.'
  if (!form.dueDate) errors.dueDate = 'Due date is required.'
  return Object.keys(errors).length === 0
}

async function focusFirstError() {
  await nextTick()
  const field = FIELDS.find((f) => errors[f])
  if (field) document.getElementById(`content-${field}`)?.focus()
}

async function submit() {
  if (saving.value) return
  if (!validate()) return focusFirstError()

  const body: ContentRequest = {
    clientId: Number(form.clientId),
    title: form.title.trim(),
    type: form.type as ContentType,
    channel: form.channel.trim() || null,
    dueDate: form.dueDate,
    notes: form.notes.trim() || null,
  }

  saving.value = true
  try {
    const editing = state.content
    const saved = editing ? await contentsApi.update(editing.id, body) : await contentsApi.create(body)
    toast.success(editing ? 'Değişiklikler kaydedildi' : 'İçerik oluşturuldu', { description: saved.title })
    notifyContentSaved(saved)
    void refreshNavCounts()
    state.open = false
  } catch (e) {
    if (e instanceof ApiError && e.status === 400 && Object.keys(e.fieldErrors).length) {
      // The server is the source of truth for validation; show its messages under the fields.
      Object.assign(errors, fieldErrors(e))
      focusFirstError()
    } else {
      toast.error('İçerik kaydedilemedi', { description: errorMessage(e) })
    }
  } finally {
    saving.value = false
  }
}

function describedBy(field: Field) {
  return errors[field] ? `content-${field}-error` : undefined
}
</script>

<template>
  <Dialog v-model:open="state.open">
    <DialogContent class="gap-6 p-6 sm:max-w-lg">
      <DialogHeader>
        <div class="flex items-center gap-2 pr-8">
          <DialogTitle class="text-lg font-semibold tracking-tight">
            {{ state.content ? 'Edit content' : 'New content' }}
          </DialogTitle>
          <StatusBadge v-if="state.content" :status="state.content.status" size="sm" />
        </div>
        <DialogDescription>
          {{ state.content
            ? 'Status changes happen on the board or in the list.'
            : 'New content starts as a draft.' }}
        </DialogDescription>
      </DialogHeader>

      <form id="content-form" class="grid gap-4" novalidate @submit.prevent="submit">
        <div class="grid gap-1.5">
          <Label for="content-clientId">Client</Label>
          <Select v-model="form.clientId">
            <SelectTrigger
              id="content-clientId"
              class="h-9 w-full"
              :aria-invalid="!!errors.clientId"
              :aria-describedby="describedBy('clientId')"
            >
              <SelectValue placeholder="Choose a client" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="client in clients" :key="client.id" :value="String(client.id)">
                {{ client.name }}
              </SelectItem>
            </SelectContent>
          </Select>
          <p v-if="errors.clientId" id="content-clientId-error" class="text-xs text-destructive">{{ errors.clientId }}</p>
        </div>

        <div class="grid gap-1.5">
          <Label for="content-title">Title</Label>
          <Input
            id="content-title"
            v-model="form.title"
            class="h-9"
            maxlength="200"
            autocomplete="off"
            placeholder="e.g. Sunset terrace reel"
            :aria-invalid="!!errors.title"
            :aria-describedby="describedBy('title')"
          />
          <p v-if="errors.title" id="content-title-error" class="text-xs text-destructive">{{ errors.title }}</p>
        </div>

        <div class="grid gap-4 sm:grid-cols-2">
          <div class="grid gap-1.5">
            <Label for="content-type">Type</Label>
            <Select v-model="form.type">
              <SelectTrigger
                id="content-type"
                class="h-9 w-full"
                :aria-invalid="!!errors.type"
                :aria-describedby="describedBy('type')"
              >
                <SelectValue placeholder="Choose a type" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="type in TYPES" :key="type" :value="type">{{ TYPE_LABELS[type] }}</SelectItem>
              </SelectContent>
            </Select>
            <p v-if="errors.type" id="content-type-error" class="text-xs text-destructive">{{ errors.type }}</p>
          </div>

          <div class="grid gap-1.5">
            <Label for="content-channel">
              Channel <span class="font-normal text-muted-foreground">(optional)</span>
            </Label>
            <Input
              id="content-channel"
              v-model="form.channel"
              class="h-9"
              maxlength="60"
              list="content-channel-suggestions"
              autocomplete="off"
              placeholder="e.g. Instagram"
              :aria-invalid="!!errors.channel"
              :aria-describedby="describedBy('channel')"
            />
            <datalist id="content-channel-suggestions">
              <option v-for="channel in CHANNEL_SUGGESTIONS" :key="channel" :value="channel" />
            </datalist>
            <p v-if="errors.channel" id="content-channel-error" class="text-xs text-destructive">{{ errors.channel }}</p>
          </div>
        </div>

        <div class="grid gap-1.5">
          <Label for="content-dueDate">Due date</Label>
          <Input
            id="content-dueDate"
            v-model="form.dueDate"
            type="date"
            class="h-9 sm:w-1/2"
            :aria-invalid="!!errors.dueDate"
            :aria-describedby="describedBy('dueDate')"
          />
          <p v-if="errors.dueDate" id="content-dueDate-error" class="text-xs text-destructive">{{ errors.dueDate }}</p>
        </div>

        <div class="grid gap-1.5">
          <Label for="content-notes">
            Notes <span class="font-normal text-muted-foreground">(optional)</span>
          </Label>
          <Textarea
            id="content-notes"
            v-model="form.notes"
            rows="3"
            maxlength="5000"
            placeholder="Brief, references, approval notes…"
            :aria-invalid="!!errors.notes"
            :aria-describedby="describedBy('notes')"
          />
          <p v-if="errors.notes" id="content-notes-error" class="text-xs text-destructive">{{ errors.notes }}</p>
        </div>
      </form>

      <DialogFooter class="-mx-6 -mb-6 gap-2 bg-transparent px-6 py-4">
        <Button type="button" variant="outline" class="h-9" :disabled="saving" @click="state.open = false">
          Cancel
        </Button>
        <Button type="submit" form="content-form" class="h-9" :disabled="saving">
          {{ saving ? 'Saving…' : state.content ? 'Save changes' : 'Create content' }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
