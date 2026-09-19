const https = require('https');

const ENDPOINT = (process.env.APPWRITE_FUNCTION_API_ENDPOINT || process.env.APPWRITE_ENDPOINT || '').replace(/\/$/, '');
const PROJECT = process.env.APPWRITE_FUNCTION_PROJECT_ID || process.env.APPWRITE_PROJECT_ID;
const API_KEY = process.env.APPWRITE_FUNCTION_API_KEY || process.env.APPWRITE_API_KEY;
const DB = 'poshan_db';

let apiKeyOverride = null;

function api(method, path, body) {
  return new Promise((resolve) => {
    const url = new URL(ENDPOINT + path);
    const payload = body ? JSON.stringify(body) : null;
    const options = {
      method,
      hostname: url.hostname,
      port: url.port || 443,
      path: url.pathname + url.search,
      headers: {
        'X-Appwrite-Project': PROJECT,
        'X-Appwrite-Key': apiKeyOverride || API_KEY,
        'Content-Type': 'application/json',
        'Content-Length': payload ? Buffer.byteLength(payload) : 0,
      },
    };
    const r = https.request(options, (res) => {
      let data = '';
      res.on('data', (c) => (data += c));
      res.on('end', () => resolve({ status: res.statusCode, body: data }));
    });
    r.on('error', (e) => resolve({ status: 0, body: String(e) }));
    if (payload) r.write(payload);
    r.end();
  });
}

// Collection-level permissions. The `label:admin` grants MUST be here rather than in the
// per-document permissions the Android client sends: Appwrite only lets a client SDK grant
// roles the calling session holds (any / users / user:<id>), and a document created with
// read("label:admin") is rejected with 401 user_unauthorized, failing every sync write.
const COLLECTION_PERMISSIONS = [
  'create("users")',
  'read("label:admin")',
  'update("label:admin")',
  'delete("label:admin")',
];

const COLLECTIONS = {
  profiles: [
    ['user_id', 'string', { size: 64, required: true }],
    ['role', 'string', { size: 32, required: true }],
    ['display_name', 'string', { size: 128, required: false, default: '' }],
    ['phone', 'string', { size: 32, required: false, default: '' }],
    ['relationship', 'string', { size: 64, required: false, default: '' }],
    ['location', 'string', { size: 256, required: false, default: '' }],
    ['anganwadi_name', 'string', { size: 128, required: false, default: '' }],
    ['anganwadi_code', 'string', { size: 16, required: false, default: '' }],
    ['facility_name', 'string', { size: 128, required: false, default: '' }],
    ['hfr_id', 'string', { size: 64, required: false, default: '' }],
    ['employee_id', 'string', { size: 64, required: false, default: '' }],
    ['is_guest', 'boolean', { required: false, default: false }],
  ],
  children: [
    ['child_id', 'string', { size: 64, required: true }],
    ['name', 'string', { size: 128, required: true }],
    ['date_of_birth_epoch_day', 'integer', { required: true }],
    ['gender', 'enum', { elements: ['MALE', 'FEMALE', 'OTHER'], required: true }],
    ['born_premature', 'boolean', { required: false, default: false }],
    ['gestational_age_weeks', 'integer', { required: false }],
    ['parent_name', 'string', { size: 128, required: false, default: '' }],
    ['parent_mobile', 'string', { size: 32, required: false, default: '' }],
    ['village', 'string', { size: 128, required: false, default: '' }],
    ['created_by_role', 'string', { size: 32, required: false, default: 'PARENT' }],
    ['owner_id', 'string', { size: 64, required: false, default: '' }],
  ],
  assessments: [
    ['assessment_id', 'string', { size: 64, required: true }],
    ['child_id', 'string', { size: 64, required: true }],
    ['assessment_date', 'integer', { required: true }],
    ['age_months', 'float', { required: false }],
    ['corrected_age_months', 'float', { required: false }],
    ['answers_json', 'string', { size: 16384, required: false, default: '[]' }],
    ['growth_json', 'string', { size: 2048, required: false, default: '' }],
    ['nutrition_json', 'string', { size: 4096, required: false, default: '' }],
    ['attention_json', 'string', { size: 4096, required: false, default: '' }],
    ['vitamin_d_json', 'string', { size: 4096, required: false, default: '' }],
    ['given_vaccines_json', 'string', { size: 4096, required: false, default: '[]' }],
    ['category_results_json', 'string', { size: 8192, required: false, default: '[]' }],
    ['composite_json', 'string', { size: 4096, required: false, default: '' }],
    ['dq', 'float', { required: false }],
    ['status', 'string', { size: 16, required: false, default: 'DRAFT' }],
    ['assessor_role', 'string', { size: 32, required: false, default: 'PARENT' }],
    ['owner_id', 'string', { size: 64, required: false, default: '' }],
  ],
  growth_records: [
    ['child_id', 'string', { size: 64, required: true }],
    ['record_date', 'integer', { required: true }],
    ['weight_kg', 'float', { required: false }],
    ['height_cm', 'float', { required: false }],
    // MUAC replaced head circumference; the old attribute stays readable for documents
    // written before the change, but nothing writes to it now.
    ['head_circumference_cm', 'float', { required: false }],
    ['muac_mm', 'float', { required: false }],
    ['owner_id', 'string', { size: 64, required: false, default: '' }],
  ],
  attention_records: [
    ['child_id', 'string', { size: 64, required: true }],
    ['task_type', 'string', { size: 32, required: false, default: 'PREFERENTIAL_LOOKING' }],
    ['valid_frame_ratio', 'float', { required: false }],
    ['on_screen_ratio', 'float', { required: false }],
    ['social_attention_ratio', 'float', { required: false }],
    ['blink_rate', 'float', { required: false }],
    ['engagement_index', 'float', { required: false }],
    ['band', 'string', { size: 16, required: false, default: 'ON_TRACK' }],
    ['inconclusive', 'boolean', { required: false, default: true }],
    ['record_date', 'integer', { required: false }],
    ['owner_id', 'string', { size: 64, required: false, default: '' }],
  ],
  vitamin_d_records: [
    ['child_id', 'string', { size: 64, required: true }],
    ['score', 'integer', { required: false }],
    ['band', 'string', { size: 16, required: false, default: 'ON_TRACK' }],
    ['factors_json', 'string', { size: 4096, required: false, default: '[]' }],
    ['record_date', 'integer', { required: false }],
    ['owner_id', 'string', { size: 64, required: false, default: '' }],
  ],
};

module.exports = async ({ req, res, log }) => {
  const report = [];

  // Appwrite exposes the function's dynamic API key on the request headers.
  const dynamicKey =
    (req && req.headers && (req.headers['x-appwrite-key'] || req.headers['X-Appwrite-Key'])) || '';
  const KEY = API_KEY || dynamicKey;

  if (!ENDPOINT || !PROJECT || !KEY) {
    return res.json({ ok: false, error: 'missing credentials', ENDPOINT, hasProject: !!PROJECT, hasKey: !!KEY }, 500);
  }

  // Re-bind the key used by api() for the remainder of the run.
  apiKeyOverride = KEY;

  let r = await api('POST', '/databases', { databaseId: DB, name: 'Poshan' });
  report.push('database: ' + r.status);

  for (const [id, attrs] of Object.entries(COLLECTIONS)) {
    r = await api('POST', '/databases/' + DB + '/collections', {
      collectionId: id,
      name: id,
      permissions: COLLECTION_PERMISSIONS,
      documentSecurity: true,
    });
    report.push('collection ' + id + ': ' + r.status);

    // Re-apply on every run so existing collections pick up permission changes.
    const p = await api('PUT', '/databases/' + DB + '/collections/' + id, {
      name: id,
      permissions: COLLECTION_PERMISSIONS,
      documentSecurity: true,
      enabled: true,
    });
    report.push('collection ' + id + ' permissions: ' + p.status);

    for (const [key, type, opts] of attrs) {
      const body = Object.assign({ key }, opts);
      const rr = await api('POST', '/databases/' + DB + '/collections/' + id + '/attributes/' + type, body);
      report.push('  ' + id + '.' + key + ' (' + type + '): ' + rr.status);
    }
  }

  log(report.join('\n'));
  return res.json({ ok: true, total: report.length, report });
};
