'use client';

import { useEffect, useState } from 'react';
import logger from '@/utils/logger';

interface AnalyticsData {
  pageViews: { date: string; views: number }[];
  userActivity: { hour: number; active: number }[];
  topPages: { path: string; views: number }[];
  userGrowth: { month: string; users: number }[];
}

export default function AdminAnalytics() {
  const [analytics, setAnalytics] = useState<AnalyticsData>({
    pageViews: [],
    userActivity: [],
    topPages: [],
    userGrowth: [],
  });
  const [loading, setLoading] = useState(true);
  const [timeRange, setTimeRange] = useState('7d');

  useEffect(() => {
    const fetchAnalytics = async () => {
      try {
        // Mock analytics data - replace with actual API calls
        const mockData: AnalyticsData = {
          pageViews: [
            { date: '2024-01-10', views: 120 },
            { date: '2024-01-11', views: 150 },
            { date: '2024-01-12', views: 180 },
            { date: '2024-01-13', views: 220 },
            { date: '2024-01-14', views: 190 },
            { date: '2024-01-15', views: 250 },
            { date: '2024-01-16', views: 280 },
          ],
          userActivity: [
            { hour: 0, active: 5 },
            { hour: 6, active: 15 },
            { hour: 12, active: 45 },
            { hour: 18, active: 35 },
            { hour: 23, active: 8 },
          ],
          topPages: [
            { path: '/', views: 1200 },
            { path: '/chat', views: 800 },
            { path: '/admin', views: 150 },
            { path: '/profile', views: 300 },
          ],
          userGrowth: [
            { month: 'Oct', users: 80 },
            { month: 'Nov', users: 95 },
            { month: 'Dec', users: 110 },
            { month: 'Jan', users: 145 },
          ],
        };
        setAnalytics(mockData);
      } catch (error) {
        logger.error('Failed to fetch analytics:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchAnalytics();
  }, [timeRange]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Analytics</h1>
          <p className="text-gray-600 mt-1">Monitor system performance and user activity</p>
        </div>
        <div className="flex items-center gap-2">
          <label htmlFor="timeRange" className="text-sm font-medium text-gray-700">
            Time Range:
          </label>
          <select
            id="timeRange"
            value={timeRange}
            onChange={(e) => setTimeRange(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="24h">Last 24 Hours</option>
            <option value="7d">Last 7 Days</option>
            <option value="30d">Last 30 Days</option>
            <option value="90d">Last 90 Days</option>
          </select>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200">
          <div className="flex items-center">
            <div className="flex-1">
              <p className="text-sm font-medium text-gray-600">Total Page Views</p>
              <p className="text-2xl font-bold text-gray-900">
                {analytics.pageViews.reduce((sum, day) => sum + day.views, 0)}
              </p>
              <p className="text-xs text-green-600">+12% from last week</p>
            </div>
            <div className="p-3 bg-blue-100 rounded-full">
              <span className="text-2xl">👁️</span>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200">
          <div className="flex items-center">
            <div className="flex-1">
              <p className="text-sm font-medium text-gray-600">Avg. Session Duration</p>
              <p className="text-2xl font-bold text-gray-900">8m 42s</p>
              <p className="text-xs text-green-600">+5% from last week</p>
            </div>
            <div className="p-3 bg-green-100 rounded-full">
              <span className="text-2xl">⏱️</span>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200">
          <div className="flex items-center">
            <div className="flex-1">
              <p className="text-sm font-medium text-gray-600">Bounce Rate</p>
              <p className="text-2xl font-bold text-gray-900">24%</p>
              <p className="text-xs text-red-600">+2% from last week</p>
            </div>
            <div className="p-3 bg-yellow-100 rounded-full">
              <span className="text-2xl">📊</span>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200">
          <div className="flex items-center">
            <div className="flex-1">
              <p className="text-sm font-medium text-gray-600">New Users</p>
              <p className="text-2xl font-bold text-gray-900">35</p>
              <p className="text-xs text-green-600">+18% from last week</p>
            </div>
            <div className="p-3 bg-purple-100 rounded-full">
              <span className="text-2xl">👤</span>
            </div>
          </div>
        </div>
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Page Views Chart */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200">
          <div className="p-6 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">Page Views</h2>
          </div>
          <div className="p-6">
            <div className="space-y-3">
              {analytics.pageViews.map((day) => (
                <div key={day.date} className="flex items-center">
                  <div className="w-20 text-sm text-gray-600">{day.date.slice(5)}</div>
                  <div className="flex-1 mx-3">
                    <div className="bg-gray-200 rounded-full h-3">
                      <div
                        className="bg-blue-600 h-3 rounded-full transition-all duration-500"
                        style={{
                          width: `${
                            (day.views / Math.max(...analytics.pageViews.map((d) => d.views))) * 100
                          }%`,
                        }}
                      ></div>
                    </div>
                  </div>
                  <div className="w-12 text-sm text-gray-900 text-right">{day.views}</div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* User Activity Chart */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200">
          <div className="p-6 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">User Activity by Hour</h2>
          </div>
          <div className="p-6">
            <div className="space-y-3">
              {analytics.userActivity.map((hour) => (
                <div key={hour.hour} className="flex items-center">
                  <div className="w-16 text-sm text-gray-600">{hour.hour}:00</div>
                  <div className="flex-1 mx-3">
                    <div className="bg-gray-200 rounded-full h-3">
                      <div
                        className="bg-green-600 h-3 rounded-full transition-all duration-500"
                        style={{
                          width: `${
                            (hour.active /
                              Math.max(...analytics.userActivity.map((h) => h.active))) *
                            100
                          }%`,
                        }}
                      ></div>
                    </div>
                  </div>
                  <div className="w-12 text-sm text-gray-900 text-right">{hour.active}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Tables Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Top Pages */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200">
          <div className="p-6 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">Top Pages</h2>
          </div>
          <div className="divide-y divide-gray-200">
            {analytics.topPages.map((page, index) => (
              <div key={page.path} className="p-4 flex items-center justify-between">
                <div className="flex items-center">
                  <div className="text-sm font-medium text-gray-900 mr-2">#{index + 1}</div>
                  <div className="text-sm text-gray-600">{page.path}</div>
                </div>
                <div className="text-sm font-medium text-gray-900">{page.views} views</div>
              </div>
            ))}
          </div>
        </div>

        {/* User Growth */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200">
          <div className="p-6 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">User Growth</h2>
          </div>
          <div className="p-6">
            <div className="space-y-3">
              {analytics.userGrowth.map((month) => (
                <div key={month.month} className="flex items-center">
                  <div className="w-12 text-sm text-gray-600">{month.month}</div>
                  <div className="flex-1 mx-3">
                    <div className="bg-gray-200 rounded-full h-3">
                      <div
                        className="bg-purple-600 h-3 rounded-full transition-all duration-500"
                        style={{
                          width: `${
                            (month.users / Math.max(...analytics.userGrowth.map((m) => m.users))) *
                            100
                          }%`,
                        }}
                      ></div>
                    </div>
                  </div>
                  <div className="w-16 text-sm text-gray-900 text-right">{month.users} users</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
