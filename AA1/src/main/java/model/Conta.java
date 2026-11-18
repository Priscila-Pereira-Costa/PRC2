package model;

public class Conta {
    private final int numero;
    private final String titular;
    private double saldo;

    public Conta(int numero, String titular, double saldo) {
        this.numero = numero;
        this.titular = titular;
        this.saldo = saldo;
    }

    public int getNumero() { return numero; }
    public String getTitular() { return titular; }
    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }
    
    public synchronized void depositar(double valor) {
        if (valor <= 0) {
            throw new IllegalArgumentException("O valor do depósito deve ser positivo.");
        }
        this.saldo += valor;
    }

    public synchronized void sacar(double valor) throws exception.SaldoInsuficienteException {
        if (valor <= 0) {
            throw new IllegalArgumentException("O valor do saque deve ser positivo.");
        }
        if (this.saldo < valor) {
            throw new exception.SaldoInsuficienteException(
                    String.format("Saldo insuficiente. Disponível: R$ %.2f", this.saldo));
        }
        this.saldo -= valor;
    }

    @Override
    public String toString() {
        return numero + " - " + titular + " | Saldo: R$ " +
               String.format("%.2f", saldo);
    }
}
