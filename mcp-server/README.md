# Guide to Creating a Google API Key and Search Engine ID (CX) for the Custom Search API

## 1️⃣ Create a Google API Key

### Step 1: Go to Google Cloud Console
- Visit: [https://console.cloud.google.com/](https://console.cloud.google.com/)
- Sign in with your Google account.

---

### Step 2: Create (or select) a Project
1. At the top, click **Select a project** → **New Project**
2. Enter a name, for example: `customsearch-demo`
3. Click **Create**

---

### Step 3: Enable the Custom Search API
1. Go to: **APIs & Services → Library**
2. In the search box, type: `Custom Search API`
3. Click **Enable**

---

### Step 4: Create an API Key
1. Go to **APIs & Services → Credentials**
2. Click **Create Credentials → API Key**
3. Google will display an API key string like:
AIzaSyD4...XYZ

## 2️⃣ Create a Search Engine ID (CX)

### Step 1: Go to Google Programmable Search Engine
 [https://programmablesearchengine.google.com/](https://programmablesearchengine.google.com/)

---

### Step 2: Create a New Search Engine
1. Click **“Add”** or **“Create a search engine”**
2. In the **Sites to search**, enter:
- `www.google.com` (for quick testing)
- or `*` if you want to allow searching across the entire web (enable this in the next step).
3. Click **Create**

---

### Step 3: Get the Search Engine ID
1. After creating your search engine, go to the management page.
2. Open **Control Panel → Details**
3. Under **Search engine ID**, you’ll see something like:
   1234567890abcdefg:abcde12345
