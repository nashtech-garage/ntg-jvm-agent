-- Insert Azure OpenAI Agent Record

INSERT INTO agent (
    id,
    name,
    description,
    avatar,
    provider,
    base_url,
    api_key,
    chat_completions_path,
    embeddings_path,
    embedding_model,
    dimension,
    model,
    temperature,
    max_tokens,
    top_p,
    frequency_penalty,
    presence_penalty,
    settings,
    version,
    created_at,
    updated_at,
    deleted_at,
    created_by,
    updated_by
) VALUES (
    gen_random_uuid(),
    'Azure OpenAI Agent',
    'Agent powered by Azure OpenAI',
    NULL,
    'AZURE_OPENAI',
    'https://<resource>.openai.azure.com/',
    'token',
    '',
    '',
    'text-embedding-3-small',
    1536,
    'gpt-4.1-mini',
    0.7,
    2048,
    1.0,
    0.0,
    0.0,
    '{"location": "eastus"}',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL,
    NULL,
    NULL
);

-- Fix provider enum value from 'Open AI' to 'OPENAI' to match ProviderType enum
UPDATE agent
SET provider = 'OPENAI'
WHERE provider = 'Open AI';

-- Note: Replace {deployment-name} with your actual Azure OpenAI deployment name
