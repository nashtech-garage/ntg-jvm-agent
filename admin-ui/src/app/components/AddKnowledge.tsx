'use client';
import React, { useTransition } from 'react';
import { useForm } from 'react-hook-form';
import { addKnowledge } from '../actions/AddKnowledgeAction';
import z from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { checkFileType, isPDFFile, isValidPDFFile } from '../utils/utils';
import { KnowledgeInput } from '../models/knowledge';

const formSchema = z.object({
  knowledge: z
    .custom<FileList>((value) => value instanceof FileList && value.length > 0, {
      error: 'File is required',
    })
    .refine(
      async (f) => checkFileType(f[0]) && (isPDFFile(f[0]) ? await isValidPDFFile(f[0]) : true),
      { error: 'File extension not supported' }
    )
    .refine((f) => f[0].size < 1_000_000, { error: 'Max size 1MB exceeded' }),
});

const AddKnowledge = () => {
  const [isPending, startTransition] = useTransition();

  const {
    register,
    handleSubmit,
    formState: { errors },
    setError,
    reset,
  } = useForm<KnowledgeInput>({ resolver: zodResolver(formSchema) });

  const onSubmit = async (data: KnowledgeInput) => {
    startTransition(async () => {
      const result = await addKnowledge(data);
      if (!result.success) {
        setError('knowledge', { message: result.error });
      } else {
        // toast here
        reset();
      }
    });
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex gap-4">
      <fieldset className="flex flex-col gap-2" disabled={isPending}>
        <input
          type="file"
          {...register('knowledge')}
          accept=".txt, .md, .pdf"
          className="p-2 border border-black rounded hover:enabled:bg-black hover:enabled:text-white cursor-pointer disabled:cursor-not-allowed"
        />
        {errors?.knowledge && (
          <span className="text-red-600 text-sm">{errors.knowledge.message}</span>
        )}
      </fieldset>
      <button
        type="submit"
        disabled={isPending}
        className="h-fit p-2 rounded border border-black px-4 cursor-pointer enabled:hover:bg-black enabled:hover:text-white disabled:cursor-not-allowed"
      >
        {isPending ? 'Submitting ...' : 'Submit'}
      </button>
    </form>
  );
};

export default AddKnowledge;
