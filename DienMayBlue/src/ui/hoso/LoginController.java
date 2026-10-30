package dieukhienController;

import truyendulieuDTO.LoginResultDTO;
import dichvuService.AuthService;
import giaodienView.xacthuc.ChangePasswordDialog;
import giaodienView.xacthuc.LoginFrame;
import giaodienView.chinh.MainFrame;

public class LoginController {

    private final LoginFrame view;
    private final AuthService authService;

    public LoginController(LoginFrame view) {
        this.view = view;
        this.authService = new AuthService();
    }

    public void login() {
        LoginResultDTO result = authService.login(
                view.getUsername(),
                view.getPassword());

        if (!result.isSuccess()) {
            view.showError(result.getMessage());
            view.clearPassword();
            return;
        }

        view.clearError();

        if (result.isRequiresPasswordChange()) {
            ChangePasswordDialog dialog =
                    new ChangePasswordDialog(
                            view,
                            authService,
                            true,
                            new Runnable() {
                                @Override
                                public void run() {
                                    openMainFrame();
                                }
                            });

            dialog.setVisible(true);
            return;
        }

        openMainFrame();
    }

    private void openMainFrame() {
        view.dispose();

        MainFrame mainFrame =
                new MainFrame(authService);

        mainFrame.setVisible(true);
    }
}
