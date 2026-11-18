package ui;

import dao.ContaDAO;
import dao.TransferenciaDAO;
import dao.TransferenciaService;
import exception.SaldoInsuficienteException;
import model.Conta;
import model.TipoTarifa;
import model.Transferencia;
import strategy.TarifaStrategyFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class PrincipalFrame extends JFrame {
    private final JTextField txtOrigem = new JTextField();
    private final JTextField txtDestino = new JTextField();
    private final JTextField txtValor = new JTextField();
    private final JComboBox<TipoTarifa> cbTarifa =
            new JComboBox<>(TipoTarifa.values());
    private final JTextField txtHistoricoConta = new JTextField();

    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Data/Hora", "Origem", "Destino", "Valor", "Tarifa", "Modalidade"}, 0);
    private final JTable tabela = new JTable(modelo);
    private final TransferenciaService service = new TransferenciaService();

    public PrincipalFrame() {
        setTitle("Banco Digital - Operações e Concorrência");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 580);
        setLocationRelativeTo(null);

        JPanel transferencia = new JPanel(new GridLayout(5, 2, 8, 8));
        transferencia.setBorder(BorderFactory.createTitledBorder("Operações ( Contas- Sacar/Depositar)"));

        transferencia.add(new JLabel("Conta origem:"));
        transferencia.add(txtOrigem);
        transferencia.add(new JLabel("Conta destino:"));
        transferencia.add(txtDestino);
        transferencia.add(new JLabel("Valor:"));
        transferencia.add(txtValor);
        transferencia.add(new JLabel("Tarifa:"));
        transferencia.add(cbTarifa);

        JButton btnTransferir = new JButton("Transferir");
        btnTransferir.addActionListener(e -> realizarTransferencia());

        JButton btnDepositar = new JButton("Depositar");
        btnDepositar.addActionListener(e -> realizarDeposito());

        JButton btnSacar = new JButton("Sacar");
        btnSacar.addActionListener(e -> realizarSaque());

        JPanel painelBotoesOp = new JPanel(new GridLayout(1, 3, 6, 0));
        painelBotoesOp.add(btnTransferir);
        painelBotoesOp.add(btnDepositar);
        painelBotoesOp.add(btnSacar);

        transferencia.add(new JLabel("Ações:"));
        transferencia.add(painelBotoesOp);

        JPanel historico = new JPanel(new BorderLayout(8, 8));
        historico.setBorder(BorderFactory.createTitledBorder("Histórico"));

        JPanel topo = new JPanel(new BorderLayout(8, 8));
        topo.add(new JLabel("Conta:"), BorderLayout.WEST);
        topo.add(txtHistoricoConta, BorderLayout.CENTER);

        JButton btnConsultar = new JButton("Consultar");
        btnConsultar.addActionListener(e -> consultarHistorico());
        topo.add(btnConsultar, BorderLayout.EAST);

        historico.add(topo, BorderLayout.NORTH);
        historico.add(new JScrollPane(tabela), BorderLayout.CENTER);

        JButton btnContas = new JButton("Listar contas");
        btnContas.addActionListener(e -> listarContas());

        JButton btnConcorrencia = new JButton("Simular Concorrência (10 Threads)");
        btnConcorrencia.addActionListener(e -> simularConcorrencia());

        JPanel rodape = new JPanel(new GridLayout(1, 2, 8, 0));
        rodape.add(btnContas);
        rodape.add(btnConcorrencia);

        setLayout(new BorderLayout(10, 10));
        add(transferencia, BorderLayout.NORTH);
        add(historico, BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
    }

    private void realizarTransferencia() {
        try {
            int origem = Integer.parseInt(txtOrigem.getText().trim());
            int destino = Integer.parseInt(txtDestino.getText().trim());
            double valor = Double.parseDouble(
                    txtValor.getText().trim().replace(",", "."));
            TipoTarifa tipo = (TipoTarifa) cbTarifa.getSelectedItem();

            Transferencia t = service.transferir(
                    origem, destino, valor, TarifaStrategyFactory.criar(tipo));

            JOptionPane.showMessageDialog(this,
                    String.format("Transferência realizada!%nTarifa: R$ %.2f",
                            t.getTarifa()));

            txtHistoricoConta.setText(String.valueOf(origem));
            consultarHistorico();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Informe números válidos.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (SaldoInsuficienteException | IllegalArgumentException | SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void realizarDeposito() {
        try {
            int conta = Integer.parseInt(txtOrigem.getText().trim());
            double valor = Double.parseDouble(
                    txtValor.getText().trim().replace(",", "."));

            Conta atualizada = service.depositar(conta, valor);

            JOptionPane.showMessageDialog(this,
                    String.format("Depósito de R$ %.2f realizado!%nNovo saldo da conta %d: R$ %.2f",
                            valor, atualizada.getNumero(), atualizada.getSaldo()));

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Informe uma Conta origem e Valor válidos.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException | SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void realizarSaque() {
        try {
            int conta = Integer.parseInt(txtOrigem.getText().trim());
            double valor = Double.parseDouble(
                    txtValor.getText().trim().replace(",", "."));

            Conta atualizada = service.sacar(conta, valor);

            JOptionPane.showMessageDialog(this,
                    String.format("Saque de R$ %.2f realizado!%nNovo saldo da conta %d: R$ %.2f",
                            valor, atualizada.getNumero(), atualizada.getSaldo()));

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Informe uma Conta origem e Valor válidos.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (SaldoInsuficienteException | IllegalArgumentException | SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void simularConcorrencia() {
        int contaTeste = 1001;
        try {
            if (!txtOrigem.getText().trim().isEmpty()) {
                contaTeste = Integer.parseInt(txtOrigem.getText().trim());
            }
        } catch (NumberFormatException ignored) {
        }

        final int numeroConta = contaTeste;

        
        new Thread(() -> {
            try {
                ContaDAO dao = new ContaDAO();
                Conta antes = dao.buscarPorNumero(numeroConta);
                if (antes == null) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                            "Conta " + numeroConta + " não encontrada.", "Erro", JOptionPane.ERROR_MESSAGE));
                    return;
                }

                double saldoInicial = antes.getSaldo();
                int totalThreads = 10; // 5 depósitos de R$ 50 e 5 saques de R$ 30 simultâneos
                CountDownLatch largada = new CountDownLatch(1);
                List<Thread> workers = new ArrayList<>();
                List<String> erros = java.util.Collections.synchronizedList(new ArrayList<>());

                for (int i = 0; i < totalThreads; i++) {
                    final boolean isDeposito = (i % 2 == 0);
                    Thread t = new Thread(() -> {
                        try {
                            largada.await(); // Todas as threads arrancam exatamente ao mesmo tempo
                            if (isDeposito) {
                                service.depositar(numeroConta, 50.0);
                            } else {
                                service.sacar(numeroConta, 30.0);
                            }
                        } catch (Exception ex) {
                            erros.add(ex.getMessage());
                        }
                    }, "Worker-Conta-" + i);
                    workers.add(t);
                    t.start();
                }

                largada.countDown();

                for (Thread t : workers) {
                    t.join();
                }

                Conta depois = dao.buscarPorNumero(numeroConta);
                double saldoEsperado = saldoInicial + (5 * 50.0) - (5 * 30.0); // + R$ 100,00 líquido

                String resumo = String.format(
                        "Teste de Concorrência (10 Threads simultâneas na conta %d)%n" +
                        "Operações: 5x Depósito R$ 50,00 e 5x Saque R$ 30,00%n%n" +
                        "Saldo Inicial: R$ %.2f%n" +
                        "Saldo Esperado: R$ %.2f%n" +
                        "Saldo Final no MySQL: R$ %.2f%n" +
                        "Erros/Bloqueios: %d",
                        numeroConta, saldoInicial, saldoEsperado, depois.getSaldo(), erros.size());

                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        resumo, "Resultado Controle de Concorrência", JOptionPane.INFORMATION_MESSAGE));

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Falha na simulação: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private void consultarHistorico() {
        try {
            int conta = Integer.parseInt(txtHistoricoConta.getText().trim());
            List<Transferencia> lista =
                    new TransferenciaDAO().listarPorConta(conta);

            modelo.setRowCount(0);
            DateTimeFormatter fmt =
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            for (Transferencia t : lista) {
                modelo.addRow(new Object[]{
                    t.getDataHora().format(fmt),
                    t.getContaOrigem(),
                    t.getContaDestino(),
                    String.format("R$ %.2f", t.getValor()),
                    String.format("R$ %.2f", t.getTarifa()),
                    t.getModalidadeTarifa()
                });
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Informe uma conta válida.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void listarContas() {
        try {
            List<Conta> contas = new ContaDAO().listar();
            String texto = String.join("\n",
                    contas.stream().map(Conta::toString).toList());
            JOptionPane.showMessageDialog(this, texto,
                    "Contas", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
