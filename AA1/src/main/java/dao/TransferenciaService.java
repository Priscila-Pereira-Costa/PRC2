package dao;

import exception.SaldoInsuficienteException;
import model.Conta;
import model.Transferencia;
import strategy.TarifaTransferenciaStrategy;

import java.sql.*;
import java.time.LocalDateTime;

public class TransferenciaService {

    public Transferencia transferir(int origem, int destino, double valor,
            TarifaTransferenciaStrategy strategy)
            throws SQLException, SaldoInsuficienteException {

        if (strategy == null)
            throw new IllegalArgumentException("Selecione uma modalidade de tarifa.");
        if (origem == destino)
            throw new IllegalArgumentException("A conta de origem e a conta de destino devem ser diferentes.");
        if (valor <= 0)
            throw new IllegalArgumentException("O valor da transferência deve ser positivo.");

        double tarifa = strategy.calcularTarifa(valor);
        double total = valor + tarifa;

        try (Connection conn = Conexao.getConnection()) {
            conn.setAutoCommit(false);

            try {
                ContaDAO contaDAO = new ContaDAO();

              
                int primeira = Math.min(origem, destino);
                int segunda = Math.max(origem, destino);

                Conta c1 = contaDAO.buscarPorNumeroComBloqueio(conn, primeira);
                Conta c2 = contaDAO.buscarPorNumeroComBloqueio(conn, segunda);

                Conta contaOrigem = (origem == primeira) ? c1 : c2;
                Conta contaDestino = (destino == primeira) ? c1 : c2;

                if (contaOrigem == null)
                    throw new SQLException("Conta de origem não existe.");
                if (contaDestino == null)
                    throw new SQLException("Conta de destino não existe.");

                
                contaOrigem.sacar(total);
                contaDestino.depositar(valor);

                contaDAO.atualizarSaldo(conn, contaOrigem.getNumero(), contaOrigem.getSaldo());
                contaDAO.atualizarSaldo(conn, contaDestino.getNumero(), contaDestino.getSaldo());

                Transferencia t = new Transferencia(
                        0, origem, destino, valor, tarifa,
                        strategy.getClass().getSimpleName(),
                        LocalDateTime.now());

                new TransferenciaDAO().inserir(conn, t);

                conn.commit();
                return t;

            } catch (SQLException | SaldoInsuficienteException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public Conta depositar(int numeroConta, double valor) throws SQLException {
        if (valor <= 0)
            throw new IllegalArgumentException("O valor do depósito deve ser positivo.");

        try (Connection conn = Conexao.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ContaDAO contaDAO = new ContaDAO();
                Conta conta = contaDAO.buscarPorNumeroComBloqueio(conn, numeroConta);
                if (conta == null)
                    throw new SQLException("Conta não encontrada: " + numeroConta);

                conta.depositar(valor);
                contaDAO.atualizarSaldo(conn, conta.getNumero(), conta.getSaldo());

                conn.commit();
                return conta;
            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public Conta sacar(int numeroConta, double valor) throws SQLException, SaldoInsuficienteException {
        if (valor <= 0)
            throw new IllegalArgumentException("O valor do saque deve ser positivo.");

        try (Connection conn = Conexao.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ContaDAO contaDAO = new ContaDAO();
                Conta conta = contaDAO.buscarPorNumeroComBloqueio(conn, numeroConta);
                if (conta == null)
                    throw new SQLException("Conta não encontrada: " + numeroConta);

                conta.sacar(valor);
                contaDAO.atualizarSaldo(conn, conta.getNumero(), conta.getSaldo());

                conn.commit();
                return conta;
            } catch (SQLException | SaldoInsuficienteException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}