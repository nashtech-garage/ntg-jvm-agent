import { NextResponse } from 'next/server';
import { SearchResponse } from '@/app/models/search-response';

export async function GET(req: Request) {
  const { searchParams } = new URL(req.url);
  const q = searchParams.get('q') || '';

  // fake data
  const result: SearchResponse = {
    id: Date.now(),
    title: `Kết quả cho "${q}" - 1`,
    body: `Fake data 1 cho ${q}`,
  };
  return NextResponse.json(result);
}
