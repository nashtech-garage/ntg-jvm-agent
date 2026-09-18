import assert from 'node:assert/strict';
import test from 'node:test';
import { dispatchSseEvent } from './sse-events.ts';

function harness() {
  const received = [];
  const handlers = {
    onToken: (value) => received.push(['message', value]),
    onReasoning: (value) => received.push(['reasoning', value]),
    onToolCall: (value) => received.push(['tool', value]),
    onQuestion: (value) => received.push(['question', value]),
    onComplete: (value) => received.push(['complete', value]),
    onError: (value) => received.push(['error', value]),
  };
  return { handlers, received };
}

test('preserves the five existing event contracts', () => {
  const { handlers, received } = harness();
  assert.equal(dispatchSseEvent({ event: 'message', data: 'hello' }, handlers, JSON.parse), false);
  assert.equal(dispatchSseEvent({ event: 'reasoning', data: 'why' }, handlers, JSON.parse), false);
  assert.equal(
    dispatchSseEvent({ event: 'tool', data: '{"id":"1","name":"x"}' }, handlers, JSON.parse),
    false
  );
  assert.equal(
    dispatchSseEvent({ event: 'complete', data: '{"id":"done"}' }, handlers, JSON.parse),
    true
  );
  assert.equal(dispatchSseEvent({ event: 'error', data: 'failed' }, handlers, JSON.parse), true);
  assert.deepEqual(
    received.map(([name]) => name),
    ['message', 'reasoning', 'tool', 'complete', 'error']
  );
});

test('a client that only knows five events ignores an unknown question event', () => {
  const { handlers, received } = harness();
  assert.equal(
    dispatchSseEvent({ event: 'future-event', data: '{}' }, handlers, JSON.parse),
    false
  );
  assert.deepEqual(received, []);
});

test('question is terminal and dispatches its payload', () => {
  const { handlers, received } = harness();
  assert.equal(
    dispatchSseEvent(
      {
        event: 'question',
        data: '{"id":"q1","conversationId":"c1","questions":[],"expiresAt":"soon"}',
      },
      handlers,
      JSON.parse
    ),
    true
  );
  assert.deepEqual(received[0], [
    'question',
    { id: 'q1', conversationId: 'c1', questions: [], expiresAt: 'soon' },
  ]);
});
