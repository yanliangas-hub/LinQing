const express = require('express');
const low = require('lowdb');
const FileSync = require('lowdb/adapters/FileSync');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const cors = require('cors');
const bodyParser = require('body-parser');
const path = require('path');
const crypto = require('crypto');

const app = express();
const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'CryptoQR-Backend-Secret-Key-2026';
const DB_PATH = process.env.DB_PATH || path.join(__dirname, 'data.json');
const ADMIN_USERNAME = process.env.ADMIN_USERNAME || 'admin';
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || 'admin123';

app.use(cors());
app.use(bodyParser.json());

// 初始化 lowdb
const adapter = new FileSync(DB_PATH);
const db = low(adapter);

db.defaults({
  users: [],
  cards: [],
  activation_records: []
}).write();

// 初始化超级管理员账号
(function initAdmin() {
  const admin = db.get('users').find({ role: 'admin' }).value();
  if (!admin) {
    const adminUser = {
      id: 'admin-' + Date.now().toString(36),
      username: ADMIN_USERNAME,
      password_hash: bcrypt.hashSync(ADMIN_PASSWORD, 10),
      role: 'admin',
      membership_type: 'admin',
      level: 99,
      points: 0,
      expiry_date: 9999999999,
      created_at: Math.floor(Date.now() / 1000)
    };
    db.get('users').push(adminUser).write();
    console.log(`[init] 已创建超级管理员账号: ${ADMIN_USERNAME}`);
  }
})();

// 工具函数
function generateCardCode() {
  return crypto.randomBytes(8).toString('hex').toUpperCase();
}

function generateToken(user) {
  return jwt.sign(
    { userId: user.id, username: user.username },
    JWT_SECRET,
    { expiresIn: '30d' }
  );
}

function authMiddleware(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ success: false, message: '未提供令牌' });
  }
  const token = authHeader.substring(7);
  try {
    req.user = jwt.verify(token, JWT_SECRET);
    next();
  } catch (err) {
    return res.status(401).json({ success: false, message: '令牌无效或已过期' });
  }
}

function adminMiddleware(req, res, next) {
  const user = db.get('users').find({ id: req.user.userId }).value();
  if (!user || user.role !== 'admin') {
    return res.status(403).json({ success: false, message: '需要管理员权限' });
  }
  next();
}

function getUserById(userId) {
  const user = db.get('users').find({ id: userId }).value();
  if (user) {
    const now = Math.floor(Date.now() / 1000);
    user.is_vip = user.membership_type !== 'free' && user.expiry_date > now;
  }
  return user;
}

function cleanUser(user) {
  if (!user) return null;
  const { password_hash, ...clean } = user;
  return clean;
}

// 注册
app.post('/api/register', (req, res) => {
  const { username, password } = req.body;
  if (!username || !password || username.length < 3 || password.length < 6) {
    return res.status(400).json({ success: false, message: '用户名至少 3 位，密码至少 6 位' });
  }

  if (username.toLowerCase() === ADMIN_USERNAME.toLowerCase()) {
    return res.status(403).json({ success: false, message: '该用户名为管理员保留，不可注册' });
  }

  const existing = db.get('users').find({ username }).value();
  if (existing) {
    return res.status(409).json({ success: false, message: '用户名已存在' });
  }

  const passwordHash = bcrypt.hashSync(password, 10);
  const newUser = {
    id: Date.now().toString(36) + Math.random().toString(36).substr(2, 5),
    username,
    password_hash: passwordHash,
    role: 'user',
    membership_type: 'free',
    level: 1,
    points: 0,
    expiry_date: 0,
    created_at: Math.floor(Date.now() / 1000)
  };

  db.get('users').push(newUser).write();
  const token = generateToken(newUser);

  res.json({ success: true, message: '注册成功', data: { token, user: cleanUser(getUserById(newUser.id)) } });
});

// 登录
app.post('/api/login', (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) {
    return res.status(400).json({ success: false, message: '请输入用户名和密码' });
  }

  const user = db.get('users').find({ username }).value();
  if (!user || !bcrypt.compareSync(password, user.password_hash)) {
    return res.status(401).json({ success: false, message: '用户名或密码错误' });
  }

  const token = generateToken(user);
  res.json({
    success: true,
    message: '登录成功',
    data: { token, user: cleanUser(getUserById(user.id)) }
  });
});

// 设备自动登录：用设备 ID 替代账号密码
app.post('/api/device-login', (req, res) => {
  const { device_id } = req.body;
  if (!device_id) {
    return res.status(400).json({ success: false, message: '缺少设备ID' });
  }

  let user = db.get('users').find({ device_id }).value();

  if (!user) {
    const randomPassword = crypto.randomBytes(16).toString('hex');
    user = {
      id: Date.now().toString(36) + Math.random().toString(36).substr(2, 5),
      username: device_id,
      password_hash: bcrypt.hashSync(randomPassword, 10),
      role: 'user',
      membership_type: 'free',
      level: 1,
      points: 0,
      expiry_date: 0,
      device_id,
      created_at: Math.floor(Date.now() / 1000)
    };
    db.get('users').push(user).write();
  }

  const token = generateToken(user);
  res.json({
    success: true,
    message: '设备登录成功',
    data: { token, user: cleanUser(getUserById(user.id)) }
  });
});

// 获取用户信息
app.get('/api/user', authMiddleware, (req, res) => {
  const user = getUserById(req.user.userId);
  if (!user) {
    return res.status(404).json({ success: false, message: '用户不存在' });
  }
  res.json({ success: true, data: { user: cleanUser(user) } });
});

// 修改用户名
app.post('/api/user/rename', authMiddleware, (req, res) => {
  const { new_username } = req.body;
  if (!new_username || new_username.length < 3) {
    return res.status(400).json({ success: false, message: '用户名至少 3 位' });
  }

  const user = getUserById(req.user.userId);
  if (!user) {
    return res.status(404).json({ success: false, message: '用户不存在' });
  }

  if (user.username === new_username) {
    return res.status(400).json({ success: false, message: '新用户名不能与当前用户名相同' });
  }

  const existing = db.get('users').find({ username: new_username }).value();
  if (existing) {
    return res.status(409).json({ success: false, message: '该用户名已被占用' });
  }

  db.get('users').find({ id: user.id }).assign({ username: new_username }).write();
  res.json({
    success: true,
    message: '用户名修改成功',
    data: { user: cleanUser(getUserById(user.id)) }
  });
});

// 兑换卡密
app.post('/api/redeem', authMiddleware, (req, res) => {
  const { card_code } = req.body;
  if (!card_code) {
    return res.status(400).json({ success: false, message: '请输入卡密' });
  }

  const card = db.get('cards').find({ card_code: card_code.toUpperCase() }).value();
  if (!card) {
    return res.status(404).json({ success: false, message: '卡密不存在' });
  }
  if (card.used) {
    return res.status(400).json({ success: false, message: '卡密已被使用' });
  }

  const user = getUserById(req.user.userId);
  if (!user) {
    return res.status(404).json({ success: false, message: '用户不存在' });
  }

  const now = Math.floor(Date.now() / 1000);
  let updates = {};

  if (card.card_type === 'membership') {
    const days = card.value;
    const newExpiry = Math.max(user.expiry_date, now) + days * 86400;
    let membershipType = 'vip';
    if (days >= 365) membershipType = 'svip';
    db.get('users').find({ id: user.id }).assign({
      membership_type: membershipType,
      expiry_date: newExpiry
    }).write();
    updates = { membership_type: membershipType, expiry_date: newExpiry, added_days: days };
  } else if (card.card_type === 'points') {
    const newPoints = user.points + card.value;
    db.get('users').find({ id: user.id }).assign({ points: newPoints }).write();
    updates = { points: newPoints, added_points: card.value };
  } else if (card.card_type === 'level') {
    const newLevel = Math.max(user.level, card.value);
    db.get('users').find({ id: user.id }).assign({ level: newLevel }).write();
    updates = { level: newLevel };
  }

  db.get('cards').find({ id: card.id }).assign({
    used: 1,
    used_by: user.id,
    used_at: now
  }).write();

  res.json({
    success: true,
    message: '卡密兑换成功',
    data: { updates, user: cleanUser(getUserById(user.id)) }
  });
});

// 设备激活（首次使用卡密激活设备）
app.post('/api/activate-device', (req, res) => {
  const { device_id, card_code } = req.body;
  if (!device_id || !card_code) {
    return res.status(400).json({ success: false, message: '缺少设备ID或卡密' });
  }

  const card = db.get('cards').find({ card_code: card_code.toUpperCase() }).value();
  if (!card) {
    return res.status(404).json({ success: false, message: '激活码无效' });
  }
  if (card.used) {
    return res.status(400).json({ success: false, message: '激活码已被使用' });
  }

  const now = Math.floor(Date.now() / 1000);
  db.get('cards').find({ id: card.id }).assign({
    used: 1,
    used_at: now
  }).write();

  db.get('activation_records').remove({ device_id }).write();
  db.get('activation_records').push({
    device_id,
    activated: 1,
    activated_at: now
  }).write();

  res.json({ success: true, message: '设备激活成功' });
});

// 检查设备激活状态
app.post('/api/check-device', (req, res) => {
  const { device_id } = req.body;
  if (!device_id) {
    return res.status(400).json({ success: false, message: '缺少设备ID' });
  }

  const record = db.get('activation_records').find({ device_id }).value();
  res.json({
    success: true,
    data: { activated: !!(record && record.activated) }
  });
});

// 生成卡密（管理接口）
app.post('/api/admin/generate-cards', authMiddleware, adminMiddleware, (req, res) => {
  const { count = 1, card_type = 'membership', value = 30 } = req.body;

  const cards = [];
  for (let i = 0; i < count; i++) {
    const code = generateCardCode();
    const card = {
      id: Date.now().toString(36) + Math.random().toString(36).substr(2, 5),
      card_code: code,
      card_type,
      value,
      used: 0,
      used_by: null,
      used_at: null,
      created_at: Math.floor(Date.now() / 1000)
    };
    db.get('cards').push(card).write();
    cards.push({ card_code: code, card_type, value });
  }

  res.json({ success: true, data: { cards } });
});

// 健康检查
app.get('/api/health', (req, res) => {
  res.json({ success: true, message: '服务正常运行' });
});

app.listen(PORT, () => {
  console.log(`CryptoQR 后端服务已启动: http://0.0.0.0:${PORT}`);
});
