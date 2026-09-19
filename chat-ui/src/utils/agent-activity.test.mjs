import assert from 'node:assert/strict';
import test from 'node:test';
import { shouldClearAgentActivity } from './agent-activity.ts';

test('preserves activity when a completed new chat transitions to its conversation route', () => {
  assert.equal(shouldClearAgentActivity('conversation-1', 'conversation-1'), false);
});

test('clears activity when opening a different conversation', () => {
  assert.equal(shouldClearAgentActivity('conversation-1', 'conversation-2'), true);
});

test('clears activity when loading a conversation without an active chat', () => {
  assert.equal(shouldClearAgentActivity(null, 'conversation-1'), true);
});
