INSERT INTO users (id, display_name, created_at) VALUES
  ('u-linxia', '林夏', '2026-01-01 00:00:00.000'),
  ('u-zhouyu', '周屿', '2026-01-01 00:00:01.000');

INSERT INTO templates (
  id, title, category, cover_url, description, required_image_count,
  accepted_mime_types, max_file_size_bytes, sort_index
) VALUES
  (
    'demo-postcard', '旅行明信片', '创意图片', '/demo/postcard.svg',
    '上传两张图片，模拟生成一张旅行明信片。', 2,
    'image/jpeg,image/png', 5242880, 1
  ),
  (
    'demo-collage', '双图拼贴', '创意图片', '/demo/collage.svg',
    '上传两张图片，模拟拼成一张双图海报。', 2,
    'image/jpeg,image/png', 5242880, 2
  ),
  (
    'demo-triptych', '三格故事', '创意图片', '/demo/triptych.svg',
    '上传三张图片，模拟排成一组三格故事。', 3,
    'image/jpeg,image/png', 5242880, 3
  ),
  (
    'demo-portrait', '人像卡片', '人像', '/demo/portrait.svg',
    '上传一张人像，模拟生成一张竖版卡片。', 1,
    'image/jpeg,image/png', 5242880, 4
  ),
  (
    'demo-product', '商品主图', '商品', '/demo/product.svg',
    '上传一张商品图，模拟生成一张主图底版。', 1,
    'image/jpeg,image/png', 5242880, 5
  );
