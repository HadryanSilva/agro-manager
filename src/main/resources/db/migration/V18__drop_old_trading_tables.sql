-- Remove tabelas do modelo antigo (PurchaseLot + LotSale).
-- Ordem: filhos antes dos pais para respeitar FK constraints.
DROP TABLE IF EXISTS lot_sale_trucks;
DROP TABLE IF EXISTS lot_sales;
DROP TABLE IF EXISTS purchase_trucks;
DROP TABLE IF EXISTS purchase_lots;
